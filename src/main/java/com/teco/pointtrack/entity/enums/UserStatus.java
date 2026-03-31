package com.teco.pointtrack.entity.enums;

/**
 * BR-22: Tài khoản NV nghỉ việc không xóa vật lý → set INACTIVE
 * ON_LEAVE: NV đang nghỉ phép – không xuất hiện trong danh sách available, không gán ca mới
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    ON_LEAVE
}
