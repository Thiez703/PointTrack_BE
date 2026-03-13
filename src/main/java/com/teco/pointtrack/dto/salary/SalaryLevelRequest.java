package com.teco.pointtrack.dto.salary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Request body cho POST (tạo mới) và PUT (cập nhật) cấp bậc lương
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalaryLevelRequest {

    @NotBlank
    @Size(max = 100)
    String name;

    @NotNull
    @DecimalMin("0.0")
    BigDecimal baseSalary;

    /** Mô tả cấp bậc (nullable) */
    String description;

    /** Trạng thái — mặc định true khi tạo mới */
    Boolean isActive;
}

