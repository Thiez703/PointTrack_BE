package com.teco.pointtrack.dto.employee;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeStatisticsResponse {

    /** Tổng nhân viên chưa bị soft delete */
    long totalEmployees;

    /** Nhân viên đang làm việc (status = ACTIVE) */
    long activeEmployees;

    /** Nhân viên đang nghỉ phép (status = ON_LEAVE) */
    long onLeaveEmployees;

    /** Nhân viên mới trong tháng hiện tại (createdAt trong tháng này) */
    long newEmployeesThisMonth;

    /**
     * Xu hướng tăng trưởng so với tháng trước.
     * Công thức: ((newThisMonth - newLastMonth) / max(newLastMonth, 1)) * 100
     * Ví dụ: "+25%", "-10%", "0%"
     */
    String totalTrend;

    /**
     * Tỷ lệ nhân viên đang làm việc trên tổng.
     * Công thức: (activeEmployees / max(totalEmployees, 1)) * 100
     * Ví dụ: "96%"
     */
    String activeRate;
}
