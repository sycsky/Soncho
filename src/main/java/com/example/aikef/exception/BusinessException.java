package com.example.aikef.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = HttpStatus.BAD_REQUEST.value();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = HttpStatus.BAD_REQUEST.value();
    }

    public int getCode() {
        return code;
    }
}
