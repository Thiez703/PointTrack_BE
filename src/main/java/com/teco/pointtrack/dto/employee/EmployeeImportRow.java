package com.teco.pointtrack.dto.employee;

import lombok.Data;

import java.util.List;

/**
 * Represents a single parsed row from the Excel import template.
 * Excel columns: Họ tên | Số điện thoại | Email | Cấp bậc | Khu vực | Ngày vào làm | Kỹ năng
 */
@Data
public class EmployeeImportRow {
    int rowNumber;
    String fullName;
    String phone;
    String email;
    String salaryLevelName;
    String area;
    String hiredDate;
    List<String> skills;
}
