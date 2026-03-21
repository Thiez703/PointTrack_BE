package com.teco.pointtrack.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleRequest {
    @NotNull(message = "userId không được trống")
    Long userId;

    @NotNull(message = "workDate không được trống")
    LocalDate workDate;

    @NotNull(message = "startTime không được trống")
    LocalTime startTime;

    @NotNull(message = "endTime không được trống")
    LocalTime endTime;

    @NotBlank(message = "address không được trống")
    String address;

    @NotNull(message = "lat không được trống")
    Double lat;

    @NotNull(message = "lng không được trống")
    Double lng;
}
