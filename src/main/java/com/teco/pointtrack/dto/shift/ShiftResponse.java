package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.ShiftType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftResponse {

    Long id;

    // employee
    Long employeeId;
    String employeeName;

    // customer
    Long   customerId;
    String customerName;
    Double customerLatitude;
    Double customerLongitude;
    String customerAddress;

    // package (nullable)
    Long packageId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate shiftDate;

    @JsonFormat(pattern = "HH:mm")
    LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    LocalTime endTime;

    Integer durationMinutes;
    ShiftType shiftType;
    BigDecimal otMultiplier;
    ShiftStatus status;
    String notes;

    // check-in
    LocalDateTime checkInTime;
    Double        checkInLat;
    Double        checkInLng;
    Double        checkInDistanceMeters;
    String        checkInPhoto;

    // check-out
    LocalDateTime checkOutTime;
    Double        checkOutLat;
    Double        checkOutLng;
    Double        checkOutDistanceMeters;

    /** Số phút thực tế làm việc (null cho đến khi check-out) */
    Integer actualMinutes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
