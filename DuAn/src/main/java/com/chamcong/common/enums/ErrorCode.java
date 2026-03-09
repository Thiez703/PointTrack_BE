package com.chamcong.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNCATEGORIZED("ERR_000", "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("ERR_001", "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("ERR_002", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_003", "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("ERR_004", "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("ERR_005", "Resource already exists", HttpStatus.CONFLICT),

    USER_NOT_FOUND("AUTH_001", "User not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("AUTH_002", "Email already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_003", "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_004", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AUTH_005", "Token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("AUTH_006", "Token has been revoked", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("AUTH_007", "Account is disabled", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_NOT_FOUND("AUTH_008", "Refresh token not found", HttpStatus.NOT_FOUND),
    PASSWORD_RESET_TOKEN_EXPIRED("AUTH_009", "Password reset token expired", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_TOKEN_USED("AUTH_010", "Password reset token already used", HttpStatus.BAD_REQUEST),
    INVALID_OLD_PASSWORD("AUTH_011", "Old password is incorrect", HttpStatus.BAD_REQUEST),
    INVALID_RESET_TOKEN("AUTH_012", "Invalid or expired reset token", HttpStatus.BAD_REQUEST),
    MAIL_SEND_FAILED("AUTH_013", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FIRST_LOGIN("AUTH_014", "Password has already been changed", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_CURRENT("AUTH_015", "New password must be different from current password", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("AUTH_016", "Password and confirm password do not match", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD_FORMAT("AUTH_017", "Password must contain at least one letter and one number", HttpStatus.BAD_REQUEST),
    INVALID_PHONE("VALID_001", "Invalid Vietnamese phone number format", HttpStatus.BAD_REQUEST),
    INVALID_URL("VALID_002", "Invalid URL format", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

