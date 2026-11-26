package com.seungwon.hrreward.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        String email,

        // 가입 정책(64자)보다 긴 값은 BCrypt 비교 전에 거른다
        @NotBlank @Size(max = 64)
        String password
) {
}
