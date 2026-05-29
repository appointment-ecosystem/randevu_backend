package com.yunus.business.dto;

import com.yunus.business.entity.BusinessStatus;
import java.util.UUID;

/**
 * İşletmenin durum bilgisini dönen DTO.
 */
public record BusinessStatusResponse(
        UUID id,
        String name,
        BusinessStatus status,
        String rejectionReason
) {
}

