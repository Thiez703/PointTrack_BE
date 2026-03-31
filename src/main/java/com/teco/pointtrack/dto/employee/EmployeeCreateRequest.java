package com.teco.pointtrack.dto.employee;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * POST /api/v1/employees – tạo nhân viên mới (ADMIN only)
 */
@Data
public class EmployeeCreateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;

    /** ID cấp bậc lương (optional – nếu null thì gán Cấp 1 mặc định) */
    Long salaryLevelId;

    /** Ngày vào làm (định dạng yyyy-MM-dd hoặc dd/MM/yyyy) */
    String hiredDate;

    /** Khu vực phụ trách */
    @Size(max = 100)
    String area;

    /** Danh sách kỹ năng, VD: ["tam_be","ve_sinh"] */
    List<String> skills;

    /** URL ảnh đại diện (optional) */
    String avatarUrl;
}
