package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FR-02: Đăng nhập bằng email + password
 * SRS dùng email, không dùng SĐT để đăng nhập
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;

    /** Turnstile captcha – dùng test key 1x000...AA khi dev */
    String captchaToken;
}
