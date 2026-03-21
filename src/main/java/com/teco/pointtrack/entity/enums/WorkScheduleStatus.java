package com.teco.pointtrack.entity.enums;

public enum WorkScheduleStatus {
    SCHEDULED,    // Ca đã được phân công, chờ NV check-in
    CONFIRMED,    // NV đã check-in thành công
    CANCELLED     // Ca đã bị hủy
}
