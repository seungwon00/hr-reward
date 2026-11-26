package com.seungwon.hrreward.auth.service;

import com.seungwon.hrreward.TestcontainersConfiguration;
import com.seungwon.hrreward.auth.dto.request.LoginRequest;
import com.seungwon.hrreward.auth.dto.request.SignUpRequest;
import com.seungwon.hrreward.auth.dto.response.SignUpResponse;
import com.seungwon.hrreward.auth.dto.response.TokenResponse;
import com.seungwon.hrreward.global.auth.JwtTokenProvider;
import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.domain.User;
import com.seungwon.hrreward.user.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceTest {

    private static final String EMAIL = "user@test.com";
    private static final String PASSWORD = "password123!";

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        redisTemplate.execute(connection -> {
            connection.serverCommands().flushAll();
            return null;
        }, true);
    }

    @Test
    void signUpStoresBcryptHashedPassword() {
        Long userId = signUp(EMAIL).userId();

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, user.getPassword())).isTrue();
    }

    @Test
    void signUpWithDuplicateEmailFails() {
        signUp(EMAIL);

        assertThatThrownBy(() -> signUp(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EMAIL_DUPLICATED);
    }

    @Test
    void loginIssuesAccessTokenAndStoresHashedRefreshTokenInRedis() {
        Long userId = signUp(EMAIL).userId();

        TokenResponse tokens = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(jwtTokenProvider.getUserId(tokens.accessToken())).isEqualTo(userId);
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.accessTokenExpiresIn()).isEqualTo(30 * 60);

        String hash = AuthService.hash(tokens.refreshToken());
        // 원문 토큰은 저장하지 않는다
        assertThat(redisTemplate.hasKey("auth:refresh:" + tokens.refreshToken())).isFalse();
        assertThat(redisTemplate.opsForValue().get("auth:refresh:" + hash)).isEqualTo(String.valueOf(userId));
        assertThat(redisTemplate.getExpire("auth:refresh:" + hash)).isBetween(14L * 24 * 3600 - 60, 14L * 24 * 3600);
        assertThat(redisTemplate.opsForSet().members("auth:user:" + userId)).containsExactly(hash);
        assertThat(redisTemplate.getExpire("auth:user:" + userId)).isPositive();
    }

    @Test
    void loginFromEachDeviceKeepsSeparateRefreshTokens() {
        Long userId = signUp(EMAIL).userId();

        TokenResponse first = authService.login(new LoginRequest(EMAIL, PASSWORD));
        TokenResponse second = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(redisTemplate.opsForSet().members("auth:user:" + userId))
                .containsExactlyInAnyOrder(AuthService.hash(first.refreshToken()), AuthService.hash(second.refreshToken()));
    }

    @Test
    void loginFailsWithSameErrorForWrongPasswordUnknownEmailAndWithdrawnUser() {
        Long userId = signUp(EMAIL).userId();
        signUp("withdrawn@test.com");
        User withdrawn = userRepository.findByEmail("withdrawn@test.com").orElseThrow();
        withdrawn.withdraw();
        userRepository.save(withdrawn);

        assertInvalidCredentials(new LoginRequest(EMAIL, "wrong-password"));
        assertInvalidCredentials(new LoginRequest("none@test.com", PASSWORD));
        assertInvalidCredentials(new LoginRequest("withdrawn@test.com", PASSWORD));
        assertThat(redisTemplate.hasKey("auth:user:" + userId)).isFalse();
    }

    @Test
    void reissueRotatesRefreshToken() {
        Long userId = signUp(EMAIL).userId();
        TokenResponse original = authService.login(new LoginRequest(EMAIL, PASSWORD));

        TokenResponse reissued = authService.reissue(original.refreshToken());

        assertThat(reissued.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(jwtTokenProvider.getUserId(reissued.accessToken())).isEqualTo(userId);
        assertThat(redisTemplate.opsForSet().members("auth:user:" + userId))
                .containsExactly(AuthService.hash(reissued.refreshToken()));

        // 한 번 사용한 refresh token은 다시 쓸 수 없다
        assertInvalidToken(() -> authService.reissue(original.refreshToken()));
        assertThat(authService.reissue(reissued.refreshToken())).isNotNull();
    }

    @Test
    void concurrentReissueWithSameTokenSucceedsOnlyOnce() throws Exception {
        signUp(EMAIL);
        String refreshToken = authService.login(new LoginRequest(EMAIL, PASSWORD)).refreshToken();
        int threads = 8;

        List<Callable<Boolean>> tasks = Collections.nCopies(threads, () -> {
            try {
                authService.reissue(refreshToken);
                return true;
            } catch (BusinessException e) {
                return false;
            }
        });
        long successCount;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            successCount = executor.invokeAll(tasks).stream().filter(AuthServiceTest::getUnchecked).count();
        }

        assertThat(successCount).isEqualTo(1);
    }

    @Test
    void reissueWithUnknownTokenFails() {
        assertInvalidToken(() -> authService.reissue("unknown-token"));
    }

    @Test
    void logoutDeletesOnlyThatDevicesRefreshToken() {
        Long userId = signUp(EMAIL).userId();
        TokenResponse phone = authService.login(new LoginRequest(EMAIL, PASSWORD));
        TokenResponse laptop = authService.login(new LoginRequest(EMAIL, PASSWORD));

        authService.logout(userId, phone.refreshToken());

        assertInvalidToken(() -> authService.reissue(phone.refreshToken()));
        assertThat(redisTemplate.opsForSet().members("auth:user:" + userId))
                .containsExactly(AuthService.hash(laptop.refreshToken()));
        assertThat(authService.reissue(laptop.refreshToken())).isNotNull();
    }

    @Test
    void logoutWithAnotherUsersTokenDoesNotDeleteIt() {
        Long attackerId = signUp("attacker@test.com").userId();
        signUp(EMAIL);
        TokenResponse victim = authService.login(new LoginRequest(EMAIL, PASSWORD));

        authService.logout(attackerId, victim.refreshToken());

        assertThat(authService.reissue(victim.refreshToken())).isNotNull();
    }

    private SignUpResponse signUp(String email) {
        return authService.signUp(new SignUpRequest(email, PASSWORD, "홍길동", "01012345678"));
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private static void assertInvalidToken(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private static boolean getUnchecked(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
