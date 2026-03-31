package com.teco.pointtrack.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception chuyên biệt cho luồng gán ca (Drag & Drop assign).
 * Mang theo errorCode và conflictDetail để ExceptionHandler trả về
 * đúng format theo đặc tả API.
 */
@Getter
public class ShiftAssignException extends RuntimeException {

    private final String     errorCode;
    private final HttpStatus httpStatus;
    private final Object     conflictDetail; // nullable

    public ShiftAssignException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode      = errorCode;
        this.httpStatus     = httpStatus;
        this.conflictDetail = null;
    }

    public ShiftAssignException(String errorCode, String message, HttpStatus httpStatus, Object conflictDetail) {
        super(message);
        this.errorCode      = errorCode;
        this.httpStatus     = httpStatus;
        this.conflictDetail = conflictDetail;
    }
}
