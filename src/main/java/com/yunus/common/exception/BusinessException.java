package com.yunus.common.exception;

import com.yunus.exception.ErrorType;

/**
 * İş kuralı ihlallerinde fırlatılan genel exception.
 * ErrorType belirtilmeden kullanıldığında HTTP 400 Bad Request olarak döner.
 * ErrorType belirtildiğinde ise GlobalExceptionHandler, ilgili ErrorType'ın
 * httpStatus alanından HTTP durum kodunu okur.
 */
public class BusinessException extends RuntimeException {

    /**
     * Hata kategorisini belirten ErrorType alanı.
     * null olabileceği için GlobalExceptionHandler'da BUSINESS_ERROR ile fallback uygulanır.
     */
    private final ErrorType errorType;

    /**
     * Sadece mesaj ile iş kuralı hatası oluşturur.
     * ErrorType olarak BUSINESS_ERROR varsayılır.
     *
     * @param message Hata mesajı
     */
    public BusinessException(String message) {
        super(message);
        this.errorType = ErrorType.BUSINESS_ERROR;
    }

    /**
     * Mesaj ve neden (cause) ile iş kuralı hatası oluşturur.
     * ErrorType olarak BUSINESS_ERROR varsayılır.
     *
     * @param message Hata mesajı
     * @param cause Asıl neden exception
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.BUSINESS_ERROR;
    }

    /**
     * ErrorType ve mesaj ile iş kuralı hatası oluşturur.
     * GlobalExceptionHandler bu constructor'dan gelen errorType üzerinden
     * hem HTTP durum kodunu hem de ErrorResponse içeriğini belirler.
     *
     * @param errorType API hata kategorisi
     * @param message   Hata mesajı
     */
    public BusinessException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Bu exception'a ait hata türünü (ErrorType) döner.
     *
     * @return Hata türü
     */
    public ErrorType getErrorType() {
        return errorType;
    }
}
