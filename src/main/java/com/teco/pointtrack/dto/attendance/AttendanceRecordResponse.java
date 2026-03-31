package com.teco.pointtrack.dto.attendance;

import com.teco.pointtrack.entity.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceRecordResponse {

    Long id;
    Long employeeId;
    String employeeName;
    Long customerId;
    String customerName;
    LocalDate shiftDate;
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    AttendanceStatus status;
    Integer lateMinutes;
    Integer earlyLeaveMinutes;
    Double distanceMeters;
    Long explanationId;
}
