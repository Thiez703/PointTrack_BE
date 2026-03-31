package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teco.pointtrack.entity.enums.ShiftType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftTemplateRequest {

    @NotBlank
    @Size(max = 100)
    String name;

    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(type = "string", example = "08:00:00", description = "Giờ bắt đầu (HH:mm:ss)")
    LocalTime defaultStart;

    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(type = "string", example = "10:00:00", description = "Giờ kết thúc (HH:mm:ss). OT_EMERGENCY cho phép < defaultStart")
    LocalTime defaultEnd;

    @NotNull
    ShiftType shiftType;

    /** Màu hex, VD: "#4CAF50". Mặc định "#4CAF50" nếu null */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color phải là mã hex hợp lệ (VD: #4CAF50)")
    String color;

    /**
     * Hệ số OT.
     * NORMAL → nên là 1.0
     * HOLIDAY → 2.0 hoặc 3.0
     * OT_EMERGENCY → 1.5
     */
    @NotNull
    @DecimalMin(value = "1.0", message = "Hệ số OT phải >= 1.0")
    @DecimalMax(value = "3.0", message = "Hệ số OT phải <= 3.0")
    BigDecimal otMultiplier;

    String notes;
}