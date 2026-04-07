package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * POST /api/v1/auth/password/forgot
 * FR-04: Gửi OTP qua SMS – hệ thống KHÔNG tiết lộ thông tin người dùng
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Thông tin đăng nhập không được để trống")
    String contact;
}
