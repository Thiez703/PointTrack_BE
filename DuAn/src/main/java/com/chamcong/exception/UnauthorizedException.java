package com.chamcong.exception;

import com.chamcong.common.enums.ErrorCode;

public class UnauthorizedException extends AppException {
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}


