package com.teco.pointtrack.dto.employee;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * GET /api/v1/employees/{id} – full profile + stats + salary history
 */
@Data
@Builder
public class EmployeeDetailResponse {
    EmployeeResponse profile;
    EmployeeStatsDto stats;
    List<SalaryHistoryResponse> salaryLevelHistory;
}
