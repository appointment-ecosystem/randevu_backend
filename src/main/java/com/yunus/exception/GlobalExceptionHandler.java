package com.yunus.exception;

import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ConflictException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.common.exception.SlotAlreadyTakenException;
import com.yunus.common.exception.UnauthorizedException;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
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
 * BusinessException ve ResourceNotFoundException artık ErrorType.getHttpStatus()
 * üzerinden HTTP durum kodu belirler; her hata türü için ayrı sabit durum kodu
 * tanımlamaya gerek yoktur.
 * Validation hatalarında birleştirilmiş alan hataları döner.
 * 500 hatalarında stack trace sadece loglanır, client'a genel mesaj döner.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation hataları (@Valid ile tetiklenir).
     * Alan bazlı hata mesajlarını birleştirerek 400 döner.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Doğrulama hatası: " + message));
    }

    /**
     * ConstraintViolation hataları (path variable ve request param validasyonları).
     * Alan bazlı ihlal mesajlarını birleştirerek 400 döner.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Doğrulama hatası: " + message));
    }

    /**
     * İş kuralı ihlalleri.
     * HTTP durum kodu, exception içindeki ErrorType.getHttpStatus() üzerinden belirlenir.
     * ErrorType null ise varsayılan olarak 400 Bad Request kullanılır.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business exception [{}]: {}", ex.getErrorType(), ex.getMessage());
        ErrorType errorType = ex.getErrorType() != null ? ex.getErrorType() : ErrorType.BUSINESS_ERROR;
        HttpStatus status = errorType.getHttpStatus();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getMessage(), errorType));
    }

    /**
     * Kaynak bulunamadı hataları.
     * HTTP durum kodu, exception içindeki ErrorType.getHttpStatus() üzerinden belirlenir.
     * ErrorType null ise varsayılan olarak 404 Not Found kullanılır.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found [{}]: {}", ex.getErrorType(), ex.getMessage());
        ErrorType errorType = ex.getErrorType() != null ? ex.getErrorType() : ErrorType.NOT_FOUND;
        HttpStatus status = errorType.getHttpStatus();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(ex.getMessage(), errorType));
    }

    /**
     * Kimlik doğrulama hatası.
     * HTTP 401 Unauthorized döner.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(ex.getMessage()));
    }

    /**
     * Yetkilendirme hatası.
     * HTTP 403 Forbidden döner.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(ex.getMessage()));
    }

    /**
     * Kaynak çakışması.
     * HTTP 409 Conflict döner.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ex.getMessage()));
    }

    /**
     * Randevu slotu çakışması.
     * HTTP 409 Conflict döner.
     */
    @ExceptionHandler(SlotAlreadyTakenException.class)
    public ResponseEntity<BaseResponse<Void>> handleSlotTaken(SlotAlreadyTakenException ex) {
        log.warn("Slot already taken: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ex.getMessage()));
    }

    /**
     * Spring Security — erişim reddedildi.
     * HTTP 403 Forbidden döner. Filter chain'den geldiği için GlobalExceptionHandler yerine
     * SecurityConfig'deki AccessDeniedHandler ile de yakalanabilir.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error("Bu işlem için yetkiniz bulunmamaktadır"));
    }

    /**
     * Spring Security — hatalı kimlik bilgileri.
     * HTTP 401 Unauthorized döner.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error("Geçersiz telefon numarası veya şifre"));
    }

    /**
     * Beklenmeyen tüm hatalar.
     * Stack trace loglanır, client'a genel mesaj döner; HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Beklenmeyen bir hata oluştu"));
    }
}
