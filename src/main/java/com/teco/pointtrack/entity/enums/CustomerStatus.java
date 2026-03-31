package com.teco.pointtrack.entity.enums;

/**
 * Trạng thái hoạt động của khách hàng.
 * <ul>
 *   <li>ACTIVE    – Đang hoạt động, có thể gán ca làm việc</li>
 *   <li>INACTIVE  – Vô hiệu hóa, không thể gán ca mới</li>
 *   <li>SUSPENDED – Tạm ngưng, không thể gán ca mới cho đến khi Admin kích hoạt lại</li>
 * </ul>
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
