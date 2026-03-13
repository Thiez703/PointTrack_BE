package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/v1/auth/accounts
 * FR-01: Admin tạo tài khoản NV – hệ thống auto-generate MK tạm và gửi email
 */
@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;

    String phoneNumber;

    String startDate; // format: yyyy-MM-dd
}
