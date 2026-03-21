package com.teco.pointtrack.dto.attendance;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleResponse {
    Long id;
    Long userId;
    String userName; // Tên hiển thị của nhân viên để FE hiển thị
    LocalDate workDate;
    LocalTime startTime;
    LocalTime endTime;
    String address;
    Double lat;
    Double lng;
}
