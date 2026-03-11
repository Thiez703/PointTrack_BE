package com.teco.pointtrack.dto.customer;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Query params cho GET /api/v1/customers
 * Hỗ trợ tìm kiếm, lọc trạng thái và phân trang
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerPageRequest {

    /** Tìm kiếm theo name hoặc phoneNumber (case-insensitive) */
    String search;

    /** Lọc theo trạng thái */
    Boolean isActive;

    /** Trang hiện tại (bắt đầu từ 0) */
    int page = 0;

    /** Số bản ghi mỗi trang */
    int size = 10;
}

