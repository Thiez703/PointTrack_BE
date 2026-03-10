package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * POST /api/v1/auth/password/forgot
 * FR-04: Gửi email link reset MK – hệ thống KHÔNG tiết lộ email có tồn tại không
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;
}
