package com.yunus.common.exception;

import com.yunus.exception.ErrorResponse;
import com.yunus.exception.ErrorType;
import com.yunus.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler sınıfının birim testleri.
 * Handler'ların doğru HTTP durum kodu ve hata mesajı döndürdüğünü doğrular.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    /**
     * BusinessException fırlatıldığında handler'ın BUSINESS_ERROR türünde ve
     * 400 Bad Request döndürdüğünü doğrular.
     */
    @Test
    void shouldHandleBusinessExceptionAndReturnBadRequest() {
        BusinessException exception = new BusinessException("Hatalı işlem talebi");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusiness(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Hatalı işlem talebi", response.getBody().getMessage());
        assertEquals(ErrorType.BUSINESS_ERROR, response.getBody().getErrorType());
    }

    /**
     * ResourceNotFoundException fırlatıldığında handler'ın NOT_FOUND türünde ve
     * 404 Not Found döndürdüğünü doğrular.
     */
    @Test
    void shouldHandleResourceNotFoundExceptionAndReturnNotFound() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Kullanıcı", "id", "12345");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Kullanıcı bulunamadı: id = '12345'", response.getBody().getMessage());
        assertEquals(ErrorType.NOT_FOUND, response.getBody().getErrorType());
    }
}
