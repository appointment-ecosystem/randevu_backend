package com.yunus.common.exception;

import com.yunus.common.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tüm uygulama genelinde fırlatılan exception'ları merkezi olarak yakalar.
 * Her exception tipine uygun HTTP status kodu ve BaseResponse formatında yanıt döner.
 * 500 hatalarında stack trace sadece loglanır, client'a genel mesaj döner.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Bean Validation hataları — @Valid ile tetiklenir
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<List<ValidationErrorResponse>>> handleValidation(
            MethodArgumentNotValidException ex) {
        List<ValidationErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationErrorResponse(err.getField(), err.getDefaultMessage()))
                .toList();
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Doğrulama hatası", errors));
    }

    // ConstraintViolation hataları — path variable ve request param validasyonları
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<List<ValidationErrorResponse>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<ValidationErrorResponse> errors = ex.getConstraintViolations().stream()
                .map(v -> new ValidationErrorResponse(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Doğrulama hatası", errors));
    }

    // İş kuralı ihlalleri
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Kaynak bulunamadı
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Kimlik doğrulama hatası
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Yetkilendirme hatası
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Kaynak çakışması
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Randevu slotu çakışması
    @ExceptionHandler(SlotAlreadyTakenException.class)
    public ResponseEntity<BaseResponse<Void>> handleSlotTaken(SlotAlreadyTakenException ex) {
        log.warn("Slot already taken: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ex.getMessage()));
    }

    // Spring Security — erişim reddedildi
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error("Bu işlem için yetkiniz bulunmamaktadır"));
    }

    // Spring Security — hatalı kimlik bilgileri
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error("Geçersiz telefon numarası veya şifre"));
    }

    // Beklenmeyen tüm hatalar — stack trace loglanır, client'a genel mesaj döner
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Beklenmeyen bir hata oluştu"));
    }
}
