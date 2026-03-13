package com.teco.pointtrack.exception;

import com.teco.pointtrack.utils.MessagesUtils;
import lombok.Setter;

@Setter
public class ConflictException extends RuntimeException {

    private String message;

    public ConflictException(String errorCode, Object... var2) {
        this.message = MessagesUtils.getMessage(errorCode, var2);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
