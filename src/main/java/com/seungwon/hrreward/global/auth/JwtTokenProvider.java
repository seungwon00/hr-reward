package com.seungwon.hrreward.global.auth;

import com.seungwon.hrreward.global.exception.BusinessException;
import com.seungwon.hrreward.global.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

// Access Token(JWT) 발급·검증. 클레임은 sub(userId), iat, exp 만 사용한다.
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenValidity;
    private final Clock clock;
    private final JwtParser parser;

    @Autowired
    public JwtTokenProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    // 만료 테스트용으로 시계를 주입할 수 있게 열어 둠
    JwtTokenProvider(JwtProperties properties, Clock clock) {
        // 32바이트 미만이면 jjwt가 WeakKeyException을 던져 기동이 실패한다
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String createAccessToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(key)
                .compact();
    }

    public Long getUserId(String accessToken) {
        try {
            String subject = parser.parseSignedClaims(accessToken).getPayload().getSubject();
            return Long.valueOf(subject);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치, 형식 오류, sub 누락·숫자 아님
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }
}
