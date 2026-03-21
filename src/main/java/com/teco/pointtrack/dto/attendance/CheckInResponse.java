package com.teco.pointtrack.dto.attendance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckInResponse {

    Long attendanceRecordId;
    AttendanceStatus status;
    LocalDateTime checkInTime;

    /** Khoảng cách thực tế từ GPS của NV đến địa điểm làm (meters) */
    Double distanceMeters;
    boolean gpsValid;

    /** Số phút đi muộn (0 = đúng giờ) */
    int lateMinutes;

    /** ID giải trình tự động tạo (nếu có) */
    Long explanationRequestId;

    /** Thông báo trả về cho FE */
    String message;
}
