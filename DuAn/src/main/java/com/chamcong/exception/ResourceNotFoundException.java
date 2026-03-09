package com.chamcong.exception;

import com.chamcong.common.enums.ErrorCode;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}


