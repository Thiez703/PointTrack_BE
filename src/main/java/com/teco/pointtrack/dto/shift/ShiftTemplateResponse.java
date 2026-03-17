package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.ShiftType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftTemplateResponse {

    Long id;
    String name;
    Integer durationMinutes;
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime defaultStart;

    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime defaultEnd;
    ShiftType shiftType;
    String color;
    BigDecimal otMultiplier;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}