package com.teco.pointtrack.dto.shift;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Chi tiết ca bị xung đột – trả về trong error response SCHEDULE_CONFLICT.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignConflictDetail {

    /** ID ca đã tồn tại gây xung đột */
    Long   existingShiftId;

    /** Khung giờ của ca xung đột, VD: "08:00-12:00" */
    String timeRange;
}
