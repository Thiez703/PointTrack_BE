package com.teco.pointtrack.dto.employee;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * POST /api/v1/employees/import – kết quả import Excel
 */
@Data
@Builder
public class ImportResult {
    int success;
    int failed;
    List<ImportError> errors;

    @Data
    @Builder
    public static class ImportError {
        int row;
        String phone;
        String email;
        String reason;
    }
}
