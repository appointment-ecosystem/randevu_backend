package com.yunus.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Validation hatalarında field bazlı hata detayını taşıyan DTO.
 * GlobalExceptionHandler tarafından kullanılır.
 */
@Getter
@Setter
@AllArgsConstructor
public class ValidationErrorResponse {

    private String field;
    private String message;
}
