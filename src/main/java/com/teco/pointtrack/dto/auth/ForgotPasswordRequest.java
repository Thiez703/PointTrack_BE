package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * POST /api/v1/auth/password/forgot
 * FR-04: Gửi OTP qua SMS – hệ thống KHÔNG tiết lộ số điện thoại có tồn tại không
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "SĐT phải 10 chữ số bắt đầu bằng 0")
    String phoneNumber;
}
