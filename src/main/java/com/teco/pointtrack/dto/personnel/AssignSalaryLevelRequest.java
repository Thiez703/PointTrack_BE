package com.teco.pointtrack.dto.personnel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignSalaryLevelRequest {
    @NotNull(message = "ID cấp bậc lương không được để trống")
    private Long salaryLevelId;
}
