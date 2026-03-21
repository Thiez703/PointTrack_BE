package com.teco.pointtrack.entity.enums;

public enum ExplanationType {
    LATE_CHECKIN,   // BR-16 (1): Đi muộn — cần Admin duyệt
    LATE_CHECKOUT,  // BR-16 (2): Checkout trễ — lý do bắt buộc ngay tại payload check-out
    GPS_INVALID     // BR-16 (3): Vị trí GPS nằm ngoài bán kính cho phép
}
