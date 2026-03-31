package com.teco.pointtrack.entity.enums;

public enum ShiftStatus {
    /** Ca nháp – Admin tạo nhưng chưa đăng */
    DRAFT,

    /** Ca đã đăng (ca trống) – chưa gán nhân viên, NV có thể đăng ký */
    PUBLISHED,

    /** Ca đã gán nhân viên – chờ NV xác nhận */
    ASSIGNED,

    /** [Legacy] Tương đương ASSIGNED – dùng cho dữ liệu cũ */
    SCHEDULED,

    /** Nhân viên đã xác nhận sẽ đi làm */
    CONFIRMED,

    /** Ca đang thực hiện (đã check-in) */
    IN_PROGRESS,

    /** Ca đã hoàn thành (đã check-out) */
    COMPLETED,

    /** Vắng mặt – quá giờ bắt đầu mà NV không check-in */
    MISSED,

    /** Thiếu check-out – NV check-in nhưng không check-out sau 2 tiếng */
    MISSING_OUT,

    /** Ca đã huỷ */
    CANCELLED
}
