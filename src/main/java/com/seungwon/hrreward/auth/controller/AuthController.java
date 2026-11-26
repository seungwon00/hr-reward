package com.seungwon.hrreward.auth.controller;

import com.seungwon.hrreward.auth.dto.request.LoginRequest;
import com.seungwon.hrreward.auth.dto.request.RefreshTokenRequest;
import com.seungwon.hrreward.auth.dto.request.SignUpRequest;
import com.seungwon.hrreward.auth.dto.response.SignUpResponse;
import com.seungwon.hrreward.auth.dto.response.TokenResponse;
import com.seungwon.hrreward.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.reissue(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Long userId, @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(userId, request.refreshToken());
    }
}
