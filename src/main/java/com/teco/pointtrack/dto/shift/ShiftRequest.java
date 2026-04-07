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

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftRequest {

    /** Nullable – nếu không truyền, ca sẽ ở trạng thái PUBLISHED (ca trống) */
    Long employeeId;

    @NotNull(message = "customerId không được để trống")
    Long customerId;

    @NotNull(message = "shiftDate không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(type = "string", example = "2026-03-21")
    LocalDate shiftDate;

    @NotNull(message = "startTime không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "08:00")
    LocalTime startTime;

    @NotNull(message = "endTime không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "10:00",
            description = "BR-10: NORMAL/HOLIDAY phải > startTime. OT_EMERGENCY cho phép < startTime (qua đêm).")
    LocalTime endTime;

    @NotNull(message = "shiftType không được để trống")
    ShiftType shiftType;

    String notes;
}
