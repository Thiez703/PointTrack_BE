package com.teco.pointtrack.dto.personnel;

import com.teco.pointtrack.entity.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request body cho PUT /api/v1/personnel/{id}
 * Chỉ dùng để cập nhật thông tin cá nhân.
 * Không bao gồm gán cấp bậc lương (đã tách ra API riêng).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEmployeeRequest {

    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    String fullName;

    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm 10 chữ số")
    String phoneNumber;

    /** Định dạng hỗ trợ: dd/MM/yyyy hoặc yyyy-MM-dd */
    String dateOfBirth;

    String avatarUrl;

    /** MALE / FEMALE / OTHER */
    Gender gender;

    /** Định dạng hỗ trợ: dd/MM/yyyy hoặc yyyy-MM-dd */
    String startDate;

    // salaryLevelId đã bị xóa khỏi đây để tách sang API riêng biệt
}
