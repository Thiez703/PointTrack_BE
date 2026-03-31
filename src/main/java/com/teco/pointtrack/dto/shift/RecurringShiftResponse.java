package com.teco.pointtrack.dto.shift;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecurringShiftResponse {

    /** Số ca đã tạo thành công */
    int created;

    /** Số ca bị bỏ qua do conflict */
    int skipped;

    /** ID của các ca vừa được tạo */
    List<Long> createdShiftIds;

    /** Chi tiết các conflict bị bỏ qua */
    List<ConflictCheckResponse> conflicts;
}
