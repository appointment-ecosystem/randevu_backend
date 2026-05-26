package com.yunus.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * API hata yanıtlarını standart formatta taşıyan DTO.
 * Validation hatalarında field bazlı detay listesini de barındırır.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private String message;
    private ErrorType errorType;
    private List<FieldError> errors;
    private OffsetDateTime timestamp;

    public ErrorResponse(String message, ErrorType errorType) {
        this.message = message;
        this.errorType = errorType;
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(String message, ErrorType errorType, List<FieldError> errors) {
        this.message = message;
        this.errorType = errorType;
        this.errors = errors;
        this.timestamp = OffsetDateTime.now();
    }

    /**
     * Alan bazlı doğrulama hata detayı.
     */
    @Getter
    @Setter
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
