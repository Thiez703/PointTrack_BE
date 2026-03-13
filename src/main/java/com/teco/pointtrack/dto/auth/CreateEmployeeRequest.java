package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * POST /api/v1/auth/accounts
 */
@Data
public class CreateEmployeeRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    String fullName;

    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm 10 chữ số")
    String phoneNumber;

    /** Hỗ trợ các định dạng: dd/MM/yyyy, yyyy-MM-dd, dd-MM-yyyy */
    String startDate;

    // salaryLevelId đã bị xóa để mặc định luôn là Cấp 1
}
