package com.yunus.common.exception;

/**
 * İş kuralı ihlallerinde fırlatılan genel exception.
 * HTTP 400 Bad Request olarak döner.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
