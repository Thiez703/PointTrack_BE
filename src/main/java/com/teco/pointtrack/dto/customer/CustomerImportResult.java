package com.teco.pointtrack.dto.customer;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response cho POST /api/v1/customers/import — Kết quả import Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerImportResult {

    /** Số dòng import thành công */
    int success;

    /** Số dòng import thất bại (validation error) */
    int failed;

    /** Số dòng import được nhưng không geocode được GPS */
    int noGps;

    /** Danh sách dòng bị lỗi (không import) */
    List<ImportError> errors;

    /** Danh sách dòng import được nhưng cần chú ý (VD: không có GPS) */
    List<ImportWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        int row;
        String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportWarning {
        int row;
        String name;
        String reason;
    }
}
