package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teco.pointtrack.entity.enums.ShiftType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload cho API gán ca trực (Drag & Drop).
 * POST /api/v1/shifts/assign
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignShiftRequest {

    @NotNull(message = "employeeId không được để trống")
    @Schema(description = "ID nhân viên được kéo thả")
    Long employeeId;

    @NotNull(message = "customerId không được để trống")
    @Schema(description = "ID khách hàng (vị trí thả)")
    Long customerId;

    @NotNull(message = "shiftDate không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(type = "string", example = "2026-03-25")
    LocalDate shiftDate;

    @Schema(description = "ID ca mẫu (tùy chọn). Nếu có, startTime/endTime lấy từ template.")
    Long templateId;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "08:00", description = "Bắt buộc nếu không dùng templateId")
    LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "12:00", description = "Bắt buộc nếu không dùng templateId")
    LocalTime endTime;

    @NotNull(message = "shiftType không được để trống")
    ShiftType shiftType;
}
