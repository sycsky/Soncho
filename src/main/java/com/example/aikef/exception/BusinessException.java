package com.example.aikef.exception;

/**
 * 通用业务异常
 * 用于在业务处理过程中抛出预期的错误信息（如：余额不足、状态不对等）
 * 全局异常处理器会捕获此异常并将 message 返回给前端
 */
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
