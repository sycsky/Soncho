package com.example.aikef.dto;

import lombok.Getter;

/**
 * A generic wrapper for all API responses.
 * @param <T> the type of the data field
 */
@Getter
public class Result<T> {

    private final int code;
    private final String message;
    private final T data;
    private final boolean success;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = (code == 200);
    }

    /**
     * Creates a success response with data.
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "Success", data);
    }

    /**
     * Creates a success response without data.
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * Creates an error response.
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
