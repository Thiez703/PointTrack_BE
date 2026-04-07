package com.teco.pointtrack.dto.packages;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackageRequest {

    @NotNull(message = "customerId không được để trống")
    Long customerId;

    @NotNull(message = "employeeId không được để trống")
    Long employeeId;

    @NotNull(message = "endTime không được để trống")
    String endTime;

    @NotNull(message = "shiftType không được để trống")
    com.teco.pointtrack.entity.enums.ShiftType shiftType;

    @NotNull(message = "otMultiplier không được để trống")
    java.math.BigDecimal otMultiplier;

    @NotNull
    @Min(value = 1, message = "Tổng buổi phải >= 1")
    Integer totalSessions;

    @NotNull(message = "recurrencePattern không được để trống")
    RecurrencePatternDto recurrencePattern;

    String notes;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecurrencePatternDto {
        /**
         * Các ngày trong tuần theo ISO: 1=Thứ Hai, …, 7=Chủ Nhật
         */
        @NotEmpty(message = "Phải chọn ít nhất 1 ngày trong tuần")
        List<Integer> days;

        /**
         * Giờ bắt đầu các buổi, VD: "08:00"
         */
        @NotNull(message = "time không được để trống")
        String time;
    }
}
