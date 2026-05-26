package com.yunus.exception;

/**
 * API hata kategorilerini tanımlayan enum.
 * GlobalExceptionHandler tarafından ErrorResponse'ta kullanılır.
 */
public enum ErrorType {
    VALIDATION_ERROR,
    BUSINESS_ERROR,
    NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    CONFLICT,
    SLOT_ALREADY_TAKEN,
    INTERNAL_SERVER_ERROR
}
