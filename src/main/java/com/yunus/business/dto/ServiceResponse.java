package com.yunus.business.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hizmet bilgisini API'ye taşıyan yanıt kaydı.
 * Entity doğrudan controller'a çıkmaz; bu DTO aracılık eder.
 */
public record ServiceResponse(
        UUID id,
        UUID businessId,
        String name,
        String description,
        Integer durationMin,
        BigDecimal price,
        String currency,
        String imageUrl,
        Boolean isActive,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
