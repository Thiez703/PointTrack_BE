package com.teco.pointtrack.dto.shift;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableEmployeeResponse {

    Long employeeId;
    String employeeName;
    String phoneNumber;

    /** Thời điểm kết thúc ca kế tiếp gần nhất (null nếu không có ca trong ngày) */
    @JsonFormat(pattern = "HH:mm")
    LocalTime nextShiftEndTime;
}
