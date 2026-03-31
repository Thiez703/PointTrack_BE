package com.teco.pointtrack.dto.shift;

import com.teco.pointtrack.entity.enums.ShiftStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Response data khi Admin gán nhân viên vào ca đã tồn tại.
 * PUT /api/v1/shifts/{shiftId}/assign
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignToShiftResponse {

    Long          id;
    Long          employeeId;
    String        employeeName;
    ShiftStatus   status;
    LocalDateTime updatedAt;
}
