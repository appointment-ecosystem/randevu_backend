package com.yunus.common.exception;

import com.yunus.common.response.BaseResponse;
import com.yunus.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleBusinessExceptionAndReturnBadRequest() {
        BusinessException exception = new BusinessException("Hatalı işlem talebi");

        ResponseEntity<BaseResponse<Void>> response = exceptionHandler.handleBusiness(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Hatalı işlem talebi", response.getBody().getMessage());
    }

    @Test
    void shouldHandleResourceNotFoundExceptionAndReturnNotFound() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Kullanıcı", "id", "12345");

        ResponseEntity<BaseResponse<Void>> response = exceptionHandler.handleNotFound(exception);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Kullanıcı bulunamadı: id = '12345'", response.getBody().getMessage());
    }
}
