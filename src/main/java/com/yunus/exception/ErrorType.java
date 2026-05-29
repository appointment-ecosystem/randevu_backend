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
    BUSINESS_NOT_FOUND,
    BUSINESS_OWNER_REQUIRED,
    BUSINESS_NOT_YOURS,
    SLUG_CONFLICT,
    WRONG_PASSWORD,
    INVALID_OTP,
    OTP_EXPIRED,
    PHONE_ALREADY_VERIFIED,
    SLOT_ALREADY_TAKEN,
    PHOTO_NOT_FOUND,
    PHOTO_UPLOAD_FAILED,
    SERVICE_NOT_FOUND,
    SERVICE_IMAGE_UPLOAD_FAILED,
    INTERNAL_SERVER_ERROR
}
