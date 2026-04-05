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
    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;
    int actualMinutes;
    int earlyLeaveMinutes;
    BigDecimal otMultiplier;

    /** Tổng số phút làm thực tế */
    Long workedMinutes;

    /** Số giờ làm thực tế (làm tròn 2 chữ số thập phân) */
    Double workedHours;

    /** Lương tạm tính (VNĐ) */
    BigDecimal estimatedSalary;

    /** Luôn là "VND" */
    String currency;

    /** Tên ca làm việc */
    String shiftName;

    String message;
}
