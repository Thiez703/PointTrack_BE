package com.teco.pointtrack.exception;

import com.teco.pointtrack.utils.MessagesUtils;
import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String errorCode;
    private String message;

    public ConflictException(String errorCode, Object... args) {
        this.errorCode = errorCode;
        this.message   = MessagesUtils.getMessage(errorCode, args);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
