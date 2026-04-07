package com.teco.pointtrack.entity.enums;

public enum AttendanceStatus {
    ON_TIME,           // Check-in đúng giờ, GPS hợp lệ
    LATE,              // Đi muộn — tự động tạo ExplanationRequest, chờ Admin duyệt (BR-16.1)
    EARLY_LEAVE,       // Về sớm hơn scheduledEnd
    ABSENT,            // Vắng mặt (không check-in)
    OVERTIME,          // Làm thêm giờ (ca OT hoặc làm vượt giờ ca thường)
    PENDING_APPROVAL   // GPS không hợp lệ — vẫn ghi nhận, chờ Admin duyệt (BR-16.3)
}
