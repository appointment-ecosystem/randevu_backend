package com.yunus.common.exception;

/**
 * Aranan kaynak bulunamadığında fırlatılan exception.
 * HTTP 404 Not Found olarak döner.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s bulunamadı: %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
