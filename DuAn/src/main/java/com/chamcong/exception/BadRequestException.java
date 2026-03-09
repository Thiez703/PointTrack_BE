package com.chamcong.exception;

import com.chamcong.common.enums.ErrorCode;

public class BadRequestException extends AppException {
    public BadRequestException() {
        super(ErrorCode.INVALID_REQUEST);
    }

    public BadRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }
}


