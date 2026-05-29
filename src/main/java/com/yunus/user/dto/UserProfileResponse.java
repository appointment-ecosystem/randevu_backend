package com.yunus.user.dto;

import com.yunus.user.entity.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Giriş yapan kullanıcının profil bilgileri.
 */
public record UserProfileResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Boolean phoneVerified,
        UserRole role,
        Boolean isActive,
        OffsetDateTime createdAt
) {
}
