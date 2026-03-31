package com.teco.pointtrack.dto.employee;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin nhân viên trả về cho danh sách (GET /v1/employees)
 * và khi tạo/cập nhật thành công.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {
    Long id;
    String fullName;
    String phone;
    String email;
    String avatarUrl;
    String area;
    List<String> skills;
    UserStatus status;
    String role;
    boolean isFirstLogin;
    LocalDate hiredDate;

    Long salaryLevelId;
    String salaryLevelName;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
