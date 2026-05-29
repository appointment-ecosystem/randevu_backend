package com.yunus.location.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * İlçe bilgisini API katmanında döndürmek için kullanılan DTO.
 */
public record DistrictResponse(
        UUID id,
        UUID cityId,
        String name,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
