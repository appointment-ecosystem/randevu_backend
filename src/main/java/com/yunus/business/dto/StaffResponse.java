package com.yunus.business.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Personel bilgisini API'ye taşıyan yanıt kaydı.
 * Entity doğrudan controller'a çıkmaz; bu DTO aracılık eder.
 * services alanı: bu personelin verebileceği hizmetleri taşır.
 */
public record StaffResponse(
        UUID id,
        UUID businessId,
        String fullName,
        String title,
        String bio,
        String profilePhotoUrl,
        Boolean isActive,
        Integer sortOrder,
        // Personelin verebileceği hizmetler; randevu ekranında slot filtrelemede kullanılır
        List<ServiceResponse> services,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
