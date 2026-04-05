package com.teco.pointtrack.dto.employee;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EmployeeProfileResponse {
    private Long id;
    private String employeeCode;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String role;
    private String avatarUrl;
    private String position;
    private String department;
    private LocalDate hiredDate;
    private String status;
    private WorkStatistics workStatistics;

    @Data
    @Builder
    public static class WorkStatistics {
        private Summary summary;
        private List<History> history;
    }

    @Data
    @Builder
    public static class Summary {
        private Long totalWorkDaysThisMonth;
        private Double totalHoursThisMonth;
        private Double otHoursThisMonth;
        private Long lateDaysThisMonth;
        private Long estimatedSalaryThisMonth;
    }

    @Data
    @Builder
    public static class History {
        private String month;
        private Long days;
    }
}
