package com.seungwon.hrreward.auth.dto.response;

import java.time.Duration;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn // 초 단위
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(String accessToken, String refreshToken, Duration accessTokenValidity) {
        return new TokenResponse(accessToken, refreshToken, BEARER, accessTokenValidity.toSeconds());
    }
}
