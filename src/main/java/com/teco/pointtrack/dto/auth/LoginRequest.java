package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * FR-02: Đăng nhập bằng số điện thoại + password
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm 10 chữ số")
    String phoneNumber;

    @NotBlank(message = "Mật khẩu không được để trống")
    String password;

    /** Turnstile captcha – dùng test key 1x000...AA khi dev */
    String captchaToken;
}
