package com.teco.pointtrack.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;

    /** Cảnh báo không nghiêm trọng — VD: geocoding thất bại nhưng record vẫn được lưu */
    private String warning;

    /** Mã lỗi nghiệp vụ — VD: SCHEDULE_CONFLICT, INACTIVE_STATUS (chỉ có khi success=false) */
    private String errorCode;

    /** Chi tiết xung đột — VD: thông tin ca bị trùng (chỉ có khi errorCode=SCHEDULE_CONFLICT) */
    private Object conflictDetail;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> successWithWarning(T data, String message, String warning) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .warning(warning)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> assignError(String errorCode, String message, Object conflictDetail) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .conflictDetail(conflictDetail)
                .build();
    }
}
