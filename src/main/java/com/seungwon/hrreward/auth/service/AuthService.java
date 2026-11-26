package com.seungwon.hrreward.auth.service;

import com.seungwon.hrreward.auth.dto.request.LoginRequest;
import com.seungwon.hrreward.auth.dto.request.SignUpRequest;
import com.seungwon.hrreward.auth.dto.response.SignUpResponse;
import com.seungwon.hrreward.auth.dto.response.TokenResponse;
import com.seungwon.hrreward.auth.repository.RefreshTokenRepository;
import com.seungwon.hrreward.global.auth.JwtProperties;
import com.seungwon.hrreward.global.auth.JwtTokenProvider;
import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import com.seungwon.hrreward.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32; // 256bit
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        Long userId = userService.signUp(request.email(), request.password(), request.name(), request.phone());
        return new SignUpResponse(userId);
    }

    public TokenResponse login(LoginRequest request) {
        Long userId = userService.authenticate(request.email(), request.password());
        return issueTokens(userId);
    }

    // 로테이션: 기존 refresh token을 꺼내면서 삭제하고 새 토큰 쌍을 발급한다.
    // 이미 사용됐거나 만료된 토큰이면 Redis에 없으므로 거부된다.
    public TokenResponse reissue(String refreshToken) {
        Long userId = refreshTokenRepository.consume(hash(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        return issueTokens(userId);
    }

    // 본인 토큰일 때만 삭제한다. 없거나 다른 사용자의 토큰이면 아무것도 하지 않는다 (멱등).
    public void logout(Long userId, String refreshToken) {
        String tokenHash = hash(refreshToken);
        refreshTokenRepository.findUserId(tokenHash)
                .filter(userId::equals)
                .ifPresent(owner -> refreshTokenRepository.delete(tokenHash, owner));
    }

    private TokenResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = generateRefreshToken();
        refreshTokenRepository.save(hash(refreshToken), userId, jwtProperties.refreshTokenValidity());
        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValidity());
    }

    private static String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Redis에는 원문 대신 SHA-256 해시를 키로 저장한다 (Redis 유출 시 토큰 재사용 방지)
    static String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
