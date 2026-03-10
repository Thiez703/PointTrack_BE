package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/v1/auth/password/reset
 * FR-04: Xác thực reset token và cập nhật MK mới (token hết hạn sau 15 phút)
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;
}
