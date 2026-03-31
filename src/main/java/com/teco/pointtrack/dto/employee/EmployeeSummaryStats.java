package com.teco.pointtrack.dto.employee;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeSummaryStats {

    /** Tổng nhân viên chưa bị xóa (kể cả INACTIVE) */
    long totalEmployees;

    /** Nhân viên đang làm việc (status = ACTIVE) */
    long activeEmployees;

    /** Nhân viên đang nghỉ phép (status = ON_LEAVE) */
    long onLeaveEmployees;

    /** Nhân viên mới trong tháng hiện tại (startDate trong tháng này) */
    long newThisMonth;
}
