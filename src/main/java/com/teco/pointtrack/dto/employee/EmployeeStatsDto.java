package com.teco.pointtrack.dto.employee;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeStatsDto {
    long totalShiftsThisMonth;
    long lateCheckinsThisMonth;
    long lateCheckoutsThisMonth;
    double completionRate;
}
