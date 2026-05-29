package com.yunus.exception;

/**
 * API hata kategorilerini tanımlayan enum.
 * GlobalExceptionHandler tarafından ErrorResponse'ta kullanılır.
 */
public enum ErrorType {
    VALIDATION_ERROR,
    BUSINESS_ERROR,
    NOT_FOUND,
    CITY_NOT_FOUND,
    DISTRICT_NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    CONFLICT,
    WRONG_PASSWORD,
    INVALID_OTP,
    OTP_EXPIRED,
    PHONE_ALREADY_VERIFIED,
    SLOT_ALREADY_TAKEN,
    INTERNAL_SERVER_ERROR
}
