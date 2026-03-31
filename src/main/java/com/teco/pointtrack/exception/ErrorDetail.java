package com.teco.pointtrack.exception;

import java.util.Collections;
import java.util.List;

public record ErrorDetail(
        String statusCode,
        String title,
        String detail,
        List<String> fieldErrors,
        String errorCode) {

    /** Constructor không có errorCode (dùng cho các lỗi không cần mã) */
    public ErrorDetail(String statusCode, String title, String detail) {
        this(statusCode, title, detail, Collections.emptyList(), null);
    }

    /** Constructor có fieldErrors nhưng không có errorCode */
    public ErrorDetail(String statusCode, String title, String detail, List<String> fieldErrors) {
        this(statusCode, title, detail, fieldErrors, null);
    }
}
