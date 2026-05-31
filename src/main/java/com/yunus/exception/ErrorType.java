package com.yunus.exception;

import org.springframework.http.HttpStatus;

/**
 * API hata kategorilerini ve karşılık gelen HTTP durum kodlarını tanımlayan enum.
 * GlobalExceptionHandler tarafından ErrorResponse oluşturulurken ve HTTP yanıt
 * kodu belirlenirken kullanılır; böylece her exception sınıfının ayrıca HTTP
 * status taşıması gerekmez.
 */
public enum ErrorType {

    // Doğrulama ve iş kuralı hataları (400)
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST),
    BUSINESS_OWNER_REQUIRED(HttpStatus.BAD_REQUEST),
    SLUG_CONFLICT(HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST),
    INVALID_OTP(HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST),
    PHOTO_UPLOAD_FAILED(HttpStatus.BAD_REQUEST),
    SERVICE_IMAGE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST),
    STAFF_PHOTO_UPLOAD_FAILED(HttpStatus.BAD_REQUEST),

    // Kimlik doğrulama hatası (401)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

    // Yetkilendirme hatası (403)
    FORBIDDEN(HttpStatus.FORBIDDEN),
    BUSINESS_NOT_YOURS(HttpStatus.FORBIDDEN),

    // Kaynak bulunamadı hataları (404)
    NOT_FOUND(HttpStatus.NOT_FOUND),
    CITY_NOT_FOUND(HttpStatus.NOT_FOUND),
    DISTRICT_NOT_FOUND(HttpStatus.NOT_FOUND),
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND),
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND),
    STAFF_NOT_FOUND(HttpStatus.NOT_FOUND),
    WORKING_HOUR_NOT_FOUND(HttpStatus.NOT_FOUND),
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND),

    // Çakışma hataları (409)
    CONFLICT(HttpStatus.CONFLICT),
    SLOT_ALREADY_TAKEN(HttpStatus.CONFLICT),
    HOLIDAY_ALREADY_EXISTS(HttpStatus.CONFLICT),

    // İstek limiti aşıldı (429)
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),

    // Sunucu hatası (500)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    /**
     * Her enum değerine karşılık gelen HTTP durum kodunu atar.
     *
     * @param httpStatus İlgili HTTP durum kodu
     */
    ErrorType(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Bu hata türüne karşılık gelen HTTP durum kodunu döner.
     *
     * @return Spring'in HttpStatus enum değeri
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
