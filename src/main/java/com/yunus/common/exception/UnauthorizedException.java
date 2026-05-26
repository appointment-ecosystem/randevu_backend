package com.yunus.common.exception;

/**
 * Kimlik doğrulama başarısız olduğunda fırlatılan exception.
 * HTTP 401 Unauthorized olarak döner.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
