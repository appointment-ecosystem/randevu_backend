package com.yunus.location.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mahalle bilgisini API katmanında döndürmek için kullanılan DTO.
 */
public record NeighborhoodResponse(
        UUID id,
        UUID districtId,
        String name,
        Boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
