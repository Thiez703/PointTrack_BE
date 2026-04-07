package com.teco.pointtrack.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tổng hợp số lượng bản ghi theo trạng thái.
 * Trả về cùng với danh sách records trong GET /attendance/history.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    long totalRecords;
    long onTime;
    long late;
    long earlyLeave;
    long absent;
    /** Bản ghi có otMultiplier > 1.0 (làm thêm giờ / ca tăng ca) */
    long overtime;
}
