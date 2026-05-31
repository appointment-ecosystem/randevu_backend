package com.yunus.common.exception;

import com.yunus.exception.ErrorType;

/**
 * Aranan kaynak bulunamadığında fırlatılan exception.
 * ErrorType belirtilmediğinde HTTP 404 Not Found olarak döner.
 * ErrorType belirtildiğinde ise GlobalExceptionHandler, ilgili ErrorType'ın
 * httpStatus alanından HTTP durum kodunu okur.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Hata kategorisini belirten ErrorType alanı.
     * null olabileceği için GlobalExceptionHandler'da NOT_FOUND ile fallback uygulanır.
     */
    private final ErrorType errorType;

    /**
     * Sadece mesaj ile kaynak bulunamadı hatası oluşturur.
     * ErrorType olarak NOT_FOUND varsayılır.
     *
     * @param message Hata mesajı
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.errorType = ErrorType.NOT_FOUND;
    }

    /**
     * Kaynak adı, alan adı ve alan değeri ile kaynak bulunamadı hatası oluşturur.
     * Standart "X bulunamadı: field = 'value'" formatında mesaj oluşturur.
     * ErrorType olarak NOT_FOUND varsayılır.
     *
     * @param resourceName Kaynak adı (örn. "İşletme")
     * @param fieldName    Alan adı (örn. "id")
     * @param fieldValue   Alan değeri (örn. UUID)
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s bulunamadı: %s = '%s'", resourceName, fieldName, fieldValue));
        this.errorType = ErrorType.NOT_FOUND;
    }

    /**
     * ErrorType ve mesaj ile kaynak bulunamadı hatası oluşturur.
     * GlobalExceptionHandler bu constructor'dan gelen errorType üzerinden
     * hem HTTP durum kodunu hem de ErrorResponse içeriğini belirler.
     *
     * @param errorType API hata kategorisi (örn. BUSINESS_NOT_FOUND)
     * @param message   Hata mesajı
     */
    public ResourceNotFoundException(ErrorType errorType, String message) {
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
