package com.yunus.common.exception;

/**
 * Yetkilendirme başarısız olduğunda fırlatılan exception.
 * Kullanıcı giriş yapmış ama ilgili işlem için yetkisi yok.
 * HTTP 403 Forbidden olarak döner.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
