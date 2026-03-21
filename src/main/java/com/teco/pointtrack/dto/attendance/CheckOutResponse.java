package com.teco.pointtrack.dto.attendance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckOutResponse {

    Long attendanceRecordId;
    AttendanceStatus status;
    LocalDateTime checkOutTime;
    int actualMinutes;
    int earlyLeaveMinutes;
    BigDecimal otMultiplier;
    String message;
}
