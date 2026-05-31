package com.yunus.business.dto;

import com.yunus.business.entity.BusinessStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin paneli işletme liste ekranında her satırda gösterilecek özet bilgileri taşır.
 * Sadece listede gereken minimum alanları içerir; detay için AdminBusinessDetailResponse kullanılır.
 */
public record AdminBusinessListResponse(

        /** İşletmenin benzersiz kimliği */
        UUID id,

        /** İşletmenin görünen adı */
        String name,

        /** SEO ve URL'de kullanılan benzersiz kısa ad */
        String slug,

        /** Admin onay / yayın durumu */
        BusinessStatus status,

        /** İşletmenin aktiflik bayrağı; false ise randevu alınamaz */
        Boolean isActive,

        /** Sahip kullanıcının tam adı */
        String ownerFullName,

        /** Sahip kullanıcının telefon numarası */
        String ownerPhone,

        /** İşletmenin bulunduğu ilin adı */
        String cityName,

        /** İşletmenin bulunduğu ilçenin adı */
        String districtName,

        /** Kaydın oluşturulma zamanı (UTC ofsetli) */
        OffsetDateTime createdAt
) {
}
