package com.teco.pointtrack.dto.personnel;

import com.teco.pointtrack.entity.enums.UserStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Query params cho GET /api/v1/personnel
 * Hỗ trợ tìm kiếm, lọc trạng thái và phân trang
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeePageRequest {

    /** Tìm kiếm theo fullName hoặc email (case-insensitive) */
    String search;

    /** Lọc theo trạng thái ACTIVE / INACTIVE */
    UserStatus status;

    /** Trang hiện tại (bắt đầu từ 0) */
    int page = 0;

    /** Số bản ghi mỗi trang */
    int size = 10;
}

