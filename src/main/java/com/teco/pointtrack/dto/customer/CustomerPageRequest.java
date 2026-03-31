package com.teco.pointtrack.dto.customer;

import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Query params cho GET /api/v1/customers — Lấy danh sách khách hàng (phân trang + lọc).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerPageRequest {

    /** Tìm kiếm theo name hoặc phone (case-insensitive) */
    String keyword;

    /** Lọc theo trạng thái (ACTIVE / INACTIVE / SUSPENDED) */
    CustomerStatus status;

    /** Lọc theo nguồn KH */
    CustomerSource source;

    /**
     * Lọc theo có hay không có tọa độ GPS.
     * true  = chỉ lấy KH có GPS.
     * false = chỉ lấy KH chưa có GPS.
     * null  = không lọc.
     */
    Boolean hasGps;

    /** Trang hiện tại (bắt đầu từ 0) */
    int page = 0;

    /** Số bản ghi mỗi trang */
    int size = 20;

    // ── Backward-compat fields ────────────────────────────────────────────────

    /** @deprecated Dùng {@link #keyword} thay thế */
    @Deprecated
    String search;

    /** @deprecated Dùng {@link #status} thay thế */
    @Deprecated
    Boolean isActive;

    /** Trả về keyword hoặc search (whichever is set) */
    public String resolvedKeyword() {
        return keyword != null ? keyword : search;
    }
}
