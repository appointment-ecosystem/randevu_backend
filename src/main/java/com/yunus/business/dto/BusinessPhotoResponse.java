package com.yunus.business.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * İşletme fotoğraf bilgisini API'ye taşıyan yanıt kaydı.
 * Entity controller katmanına çıkmaz; bu DTO aracılık eder.
 */
public record BusinessPhotoResponse(
        UUID id,
        String url,
        String fileName,
        Integer fileSize,
        String mimeType,
        Boolean isCover,
        Integer sortOrder,
        OffsetDateTime createdAt
) {
}
