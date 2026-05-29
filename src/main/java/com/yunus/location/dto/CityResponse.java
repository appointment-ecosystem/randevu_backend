package com.yunus.location.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * İl bilgisini API katmanında döndürmek için kullanılan DTO.
 */
public record CityResponse(
        UUID id,
        String name,
        String code,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
