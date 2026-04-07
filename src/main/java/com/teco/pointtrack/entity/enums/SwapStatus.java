package com.teco.pointtrack.entity.enums;

public enum SwapStatus {
    /** Chờ NV nhận (NV_B) chấp nhận / từ chối */
    PENDING_EMPLOYEE,

    /** Chờ Admin duyệt */
    PENDING_ADMIN,

    /** Đã được duyệt và thực hiện đổi ca */
    APPROVED,

    /** Bị từ chối (NV_B hoặc Admin) */
    REJECTED,

    /** Bị hủy (NV_A hủy chủ động hoặc tự động hết hạn) */
    CANCELLED
}
