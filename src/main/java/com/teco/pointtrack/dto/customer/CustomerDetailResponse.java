package com.teco.pointtrack.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response cho GET /api/v1/customers/{id} — Chi tiết đầy đủ + thống kê + ca gần đây.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerDetailResponse {

    // ── Thông tin khách hàng ──────────────────────────────────────────────────

    Long id;
    String name;
    String phone;
    String secondaryPhone;
    String address;
    Double latitude;
    Double longitude;
    boolean hasGps;
    String specialNotes;
    String preferredTimeNote;
    CustomerSource source;
    CustomerStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // ── Thống kê ─────────────────────────────────────────────────────────────

    CustomerStats stats;

    // ── 10 ca gần nhất ───────────────────────────────────────────────────────

    List<RecentShift> recentShifts;

    // ── Inner types ───────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerStats {
        long totalShifts;
        long completedShifts;
        long activePackages;
        long totalLateCheckouts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentShift {
        Long shiftId;
        String employeeName;
        LocalDate shiftDate;
        String status;
    }
}
