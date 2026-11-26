package com.seungwon.hrreward.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email @Size(max = 255)
        String email,

        // BCrypt는 72바이트까지만 처리하므로 ASCII 출력 문자로 제한 (64자 = 64바이트)
        @NotBlank
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @Pattern(regexp = "^[\\x21-\\x7E]+$", message = "비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다.")
        String password,

        @NotBlank @Size(max = 50)
        String name,

        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "휴대폰 번호는 하이픈 없이 숫자만 입력해야 합니다.")
        String phone
) {
}
