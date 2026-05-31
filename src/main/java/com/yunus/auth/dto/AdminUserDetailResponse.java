package com.yunus.auth.dto;

import com.yunus.user.entity.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin paneli için kullanıcı detay yanıt DTO'su.
 * Kullanıcının tüm profil bilgilerini, rolünü ve aktiflik durumunu içerir.
 * passwordHash gibi hassas alanlar bu DTO'ya dahil edilmez.
 */
public record AdminUserDetailResponse(

        /** Kullanıcının benzersiz kimliği. */
        UUID id,

        /** Kullanıcının tam adı. */
        String fullName,

        /** Kullanıcının e-posta adresi; kayıt sırasında girilmemişse null olabilir. */
        String email,

        /** Kullanıcının telefon numarası; birincil kimlik alanıdır. */
        String phone,

        /** Telefon numarasının OTP ile doğrulanıp doğrulanmadığı. */
        Boolean phoneVerified,

        /** Kullanıcının sistemdeki rol tipi (USER, BUSINESS_OWNER, ADMIN vb.). */
        UserRole role,

        /** Kullanıcının aktif olup olmadığı; false ise soft-delete uygulanmıştır. */
        Boolean isActive,

        /** Profil fotoğrafının URL'si; yüklenmemişse null olabilir. */
        String profilePhotoUrl,

        /** Kaydın ilk oluşturulduğu tarih ve saat. */
        OffsetDateTime createdAt,

        /** Kaydın son güncellendiği tarih ve saat. */
        OffsetDateTime updatedAt
) {
}
