package com.seungwon.hrreward.global.auth;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "aHItcmV3YXJkLXRlc3Qtb25seS1qd3Qtc2VjcmV0LW5vdC1mb3ItcHJvZHVjdGlvbg==";
    private static final String OTHER_SECRET = "YW5vdGhlci10ZXN0LW9ubHktand0LXNlY3JldC1mb3ItZm9yZ2VyeS1jaGVja3MhIQ==";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final JwtTokenProvider provider = providerAt(SECRET, NOW);

    @Test
    void createdTokenContainsOnlyUserIdAsSubject() {
        String token = provider.createAccessToken(42L);

        assertThat(provider.getUserId(token)).isEqualTo(42L);

        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload).isEqualTo("{\"sub\":\"42\",\"iat\":1767225600,\"exp\":1767227400}");
    }

    @Test
    void tokenIsValidUntilExpiration() {
        String token = provider.createAccessToken(1L);

        assertThat(providerAt(SECRET, NOW.plus(Duration.ofMinutes(29))).getUserId(token)).isEqualTo(1L);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = provider.createAccessToken(1L);
        JwtTokenProvider later = providerAt(SECRET, NOW.plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> later.getUserId(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = providerAt(OTHER_SECRET, NOW).createAccessToken(1L);

        assertThatThrownBy(() -> provider.getUserId(forged))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> provider.getUserId("not-a-jwt"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void unsignedTokenIsRejected() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\"}".getBytes());

        assertThatThrownBy(() -> provider.getUserId(header + "." + payload + "."))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void secretShorterThan256BitsIsRejectedAtStartup() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short-secret".getBytes());

        assertThatThrownBy(() -> providerAt(shortSecret, NOW))
                .isInstanceOf(WeakKeyException.class);
    }

    private static JwtTokenProvider providerAt(String secret, Instant now) {
        JwtProperties properties = new JwtProperties(secret, Duration.ofMinutes(30), Duration.ofDays(14));
        return new JwtTokenProvider(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}
