package com.yunus.business.dto;

import com.yunus.business.entity.BusinessStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin paneli işletme detay ekranında gösterilecek tüm bilgileri taşır.
 * AdminBusinessListResponse'taki alanlara ek olarak iletişim, adres ve kategori
 * bilgilerini de içerir; tek kayıt sorgusunda admin'e tam görünüm sunar.
 */
public record AdminBusinessDetailResponse(

        /** İşletmenin benzersiz kimliği */
        UUID id,

        /** İşletmenin görünen adı */
        String name,

        /** SEO ve URL'de kullanılan benzersiz kısa ad */
        String slug,

        /** Admin onay / yayın durumu */
        BusinessStatus status,

        /** İşletmenin aktiflik bayrağı */
        Boolean isActive,

        /** Sahip kullanıcının tam adı */
        String ownerFullName,

        /** Sahip kullanıcının telefon numarası */
        String ownerPhone,

        /** İşletmenin bulunduğu ilin adı */
        String cityName,

        /** İşletmenin bulunduğu ilçenin adı */
        String districtName,

        /** Kaydın oluşturulma zamanı */
        OffsetDateTime createdAt,

        // --- Detay alanları ---

        /** Admin red kararında girilen gerekçe metni */
        String rejectionReason,

        /** İşletme tanıtım metni */
        String description,

        /** İşletmenin iletişim telefonu */
        String phone,

        /** İşletmenin iletişim e-postası */
        String email,

        /** İşletmenin web sitesi adresi */
        String website,

        /** Mahallenin adı */
        String neighborhoodName,

        /** Sokak, cadde, bina no gibi serbest adres satırı */
        String addressLine,

        /** Coğrafi enlem koordinatı */
        Double latitude,

        /** Coğrafi boylam koordinatı */
        Double longitude,

        /** İşletmenin bağlı olduğu kategorilerin ad listesi */
        List<String> categoryNames,

        /** Kaydın son güncellenme zamanı */
        OffsetDateTime updatedAt
) {
}
