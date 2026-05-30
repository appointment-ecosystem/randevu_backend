package com.yunus.business.dto;

// Sınıf adı: BusinessDiscoveryResponse
// Amacı: Keşif ve arama sonuçlarında işletme özetini taşır.
// Ne yapıyor: İşletme adı, şehir, kapak fotoğrafı, ortalama puan ve kullanıcıya
//             olan mesafe (Haversine — servis katmanında doldurulur) gibi alanları
//             tek bir immutable record içinde sunar. Liste sayfaları için tasarlanmıştır.

import java.util.List;
import java.util.UUID;

/**
 * İşletme keşif / arama endpoint'lerinin yanıt nesnesi.
 *
 * <p>{@code distanceKm} alanı servis katmanında {@code HaversineUtil.calculate()}
 * ile hesaplanarak doldurulur; repository'den null gelir.
 *
 * <p>{@code coverPhotoUrl} business_photos tablosunda {@code is_cover = true}
 * olan kayıttan gelir; yoksa {@code null} döner.
 */
public record BusinessDiscoveryResponse(

        UUID id,

        String name,

        String slug,

        String description,

        /** İşletmenin bulunduğu ilin adı (örn. "İstanbul"). */
        String cityName,

        /** İlçe adı (örn. "Kadıköy"). */
        String districtName,

        /** Kapak fotoğrafının URL'si; is_cover=true olan photo kaydından gelir, yoksa null. */
        String coverPhotoUrl,

        /** İşletmenin görünür yorumlardan hesaplanan ortalama puanı; yorum yoksa null. */
        Double averageRating,

        /** Görünür (isVisible=true) yorum sayısı. */
        Long reviewCount,

        Double latitude,

        Double longitude,

        /**
         * Kullanıcının talep ettiği koordinata olan mesafe (km).
         * Servis katmanında HaversineUtil ile hesaplanır; repository'den 0.0 veya null gelebilir.
         */
        Double distanceKm,

        /** İşletmenin ait olduğu kategori adlarının listesi (örn. ["Berber", "Kuaför"]). */
        List<String> categoryNames

) {}
