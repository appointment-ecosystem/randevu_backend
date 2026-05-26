package com.yunus.common.exception;

/**
 * Kaynak çakışması durumunda fırlatılan exception.
 * Örneğin aynı telefon numarasıyla tekrar kayıt denemesi.
 * HTTP 409 Conflict olarak döner.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
