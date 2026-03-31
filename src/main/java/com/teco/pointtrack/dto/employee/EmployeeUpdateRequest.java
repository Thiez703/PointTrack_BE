package com.teco.pointtrack.dto.employee;

import com.teco.pointtrack.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * PUT /api/v1/employees/{id} – cập nhật thông tin nhân viên (ADMIN only)
 */
@Data
public class EmployeeUpdateRequest {

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    String fullName;

    @Email(message = "Email không hợp lệ")
    String email;

    /** Khu vực phụ trách */
    @Size(max = 100)
    String area;

    /** Danh sách kỹ năng */
    List<String> skills;

    /** URL ảnh đại diện */
    String avatarUrl;

    /** Trạng thái nhân viên */
    UserStatus status;

    /**
     * ID cấp bậc lương mới.
     * Nếu khác cấp hiện tại → bắt buộc phải có salaryChangeReason.
     */
    Long salaryLevelId;

    /**
     * Lý do thay đổi cấp bậc – bắt buộc khi salaryLevelId thay đổi.
     */
    String salaryChangeReason;

    /**
     * Ngày hiệu lực của thay đổi cấp bậc.
     * Mặc định = hôm nay nếu không truyền.
     */
    LocalDate salaryEffectiveDate;
}
