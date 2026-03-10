package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PUT /api/v1/auth/password/first-change
 * FR-03: Bắt buộc đổi MK tạm trong lần đăng nhập đầu tiên
 * Chỉ được gọi khi is_first_login = true
 */
@Data
public class FirstChangePasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;
}
