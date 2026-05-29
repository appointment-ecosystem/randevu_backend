package com.yunus.business.dto;

import com.yunus.business.entity.BusinessStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * İşletme detaylarını API katmanında döndürmek için kullanılan DTO.
 */
public record BusinessResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String phone,
        String email,
        String website,
        UUID cityId,
        String cityName,
        UUID districtId,
        String districtName,
        UUID neighborhoodId,
        String neighborhoodName,
        String addressLine,
        Double latitude,
        Double longitude,
        BusinessStatus status,
        String rejectionReason,
        Boolean isActive,
        String ownerName,
        List<String> categories,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

