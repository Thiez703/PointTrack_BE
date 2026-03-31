package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teco.pointtrack.entity.enums.ShiftType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Request tạo ca lặp lại theo lịch tuần trong khoảng thời gian.
 * VD: NV A làm tại KH X vào Thứ 2-4-6 từ 08:00-16:00, từ 01/04 đến 30/06.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecurringShiftRequest {

    /** Nullable – nếu null, ca được tạo ở trạng thái PUBLISHED (ca trống) */
    Long employeeId;

    @NotNull(message = "customerId không được để trống")
    Long customerId;

    Long templateId;

    @NotNull(message = "startDate không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(type = "string", example = "2026-04-01")
    LocalDate startDate;

    @NotNull(message = "endDate không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(type = "string", example = "2026-06-30",
            description = "Ngày kết thúc (bao gồm). Tối đa 180 ngày từ startDate.")
    LocalDate endDate;

    @NotEmpty(message = "daysOfWeek không được để trống")
    @Schema(description = "Các thứ trong tuần: MONDAY, TUESDAY, ..., SUNDAY",
            example = "[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]")
    Set<DayOfWeek> daysOfWeek;

    @NotNull(message = "startTime không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "08:00")
    LocalTime startTime;

    @NotNull(message = "endTime không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "16:00")
    LocalTime endTime;

    @NotNull(message = "shiftType không được để trống")
    ShiftType shiftType;

    String notes;
}
