package com.teco.pointtrack.dto.attendance;

import com.teco.pointtrack.entity.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Response DTO cho API lịch sử chấm công dành cho Admin.
 * Khớp với cấu trúc nested theo yêu cầu của FE contract.
 */
@Getter
@Builder
public class AttendanceHistoryResponse {

    Long id;

    EmployeeInfo employee;
    LocationInfo location;
    ShiftInfo shift;

    LocalDate date;

    CheckInfo checkIn;
    CheckInfo checkOut;

    Integer totalMinutes;
    Integer overtimeMinutes;
    AttendanceStatus status;
    String note;

    @Getter
    @Builder
    public static class EmployeeInfo {
        Long id;
        String name;
        String code;
        String avatar;
        String department;
    }

    @Getter
    @Builder
    public static class LocationInfo {
        Long id;
        String name;
        String address;
    }

    @Getter
    @Builder
    public static class ShiftInfo {
        String name;
        String startTime; // HH:mm
        String endTime;   // HH:mm
        String type;      // MORNING, AFTERNOON, NIGHT
    }

    @Getter
    @Builder
    public static class CheckInfo {
        String time;      // HH:mm
        String method;    // GPS, QR, ...
        String note;
        Integer lateMinutes;   // Dùng cho checkIn
        Integer earlyMinutes;  // Dùng cho checkOut
    }
}
