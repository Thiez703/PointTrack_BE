package com.teco.pointtrack.entity.enums;

public enum WorkScheduleStatus {
    SCHEDULED,    // Ca đã được phân công, chờ NV check-in
    IN_PROGRESS,  // Ca đang thực hiện (đã check-in)
    CONFIRMED,    // [Legacy] NV đã check-in thành công
    COMPLETED,    // Ca đã hoàn thành (đã check-out)
    CANCELLED     // Ca đã bị hủy
}
