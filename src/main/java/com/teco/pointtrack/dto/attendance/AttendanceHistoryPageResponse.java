package com.teco.pointtrack.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response tổng hợp cho GET /attendance/history.
 *
 * <p>Format FE contract:
 * <pre>
 * {
 *   "success": true,
 *   "data": {
 *     "records":    [...],
 *     "pagination": { "page", "limit", "total", "totalPages" },
 *     "summary":    { "totalRecords", "onTime", "late", "earlyLeave", "absent", "overtime" }
 *   }
 * }
 * </pre>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceHistoryPageResponse {

    List<AttendanceHistoryResponse> records;
    PaginationMeta pagination;
    AttendanceSummaryResponse summary;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMeta {
        int page;
        int limit;
        long total;
        int totalPages;
    }
}
