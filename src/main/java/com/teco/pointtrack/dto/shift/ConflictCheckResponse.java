package com.teco.pointtrack.dto.shift;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Kết quả kiểm tra conflict theo BR-13.
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConflictCheckResponse {

    boolean hasConflict;

    /**
     * "OVERLAP" – Ca trùng giờ (hard block)
     * "BUFFER"  – Khoảng cách giữa 2 ca < thời gian di chuyển tối thiểu
     * null      – Không có conflict
     */
    String conflictType;

    /**
     * Mô tả chi tiết, VD:
     * "Ca đề xuất 08:00-10:00 trùng giờ với ca hiện tại 09:00-11:00 (id=15)"
     * "Ca hiện tại (id=15) kết thúc 10:00, ca đề xuất bắt đầu 10:10 → cần 15 phút, thiếu 5 phút"
     */
    String detail;

    /** ID của ca bị xung đột (null nếu không có conflict) */
    Long conflictingShiftId;

    /** Số phút thiếu (chỉ có khi conflictType = BUFFER) */
    Integer minutesShort;
}
