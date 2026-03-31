package com.teco.pointtrack.dto.employee;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SalaryHistoryResponse {
    Long id;
    Long employeeId;
    String employeeName;

    Long oldLevelId;
    String oldLevelName;

    Long newLevelId;
    String newLevelName;

    LocalDate effectiveDate;
    String reason;

    Long changedById;
    String changedByName;

    LocalDateTime createdAt;
}
