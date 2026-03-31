package com.teco.pointtrack.dto.shift;

import com.teco.pointtrack.entity.enums.ShiftStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Response data khi gán ca thành công.
 * Dùng trong field {@code data} của ApiResponse.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignShiftResponse {

    Long       shiftId;
    String     employeeName;
    String     customerName;
    ShiftStatus status;
}
