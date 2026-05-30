package com.yunus.business.dto;

// Sınıf adı: BusinessDiscoveryDetailResponse
// Amacı: İşletme detay sayfasında gösterilecek tüm bilgileri taşır.
// Ne yapıyor: BusinessDiscoveryResponse'taki özet alanlara ek olarak iletişim bilgileri,
//             mahalle, aktif hizmet listesi (ServiceSummary) ve aktif personel listesi
//             (StaffSummary) döner. Detay endpoint'i için tasarlanmıştır.

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * İşletme detay sayfası endpoint'inin yanıt nesnesi.
 *
 * <p>İç record'lar {@link ServiceSummary} ve {@link StaffSummary},
 * sırasıyla aktif hizmetleri ve aktif personeli temsil eder.
 *
 * <p>{@code distanceKm} servis katmanında HaversineUtil ile doldurulur.
 */
public record BusinessDiscoveryDetailResponse(

        // ── Kimlik ve temel bilgiler ──────────────────────────────────────────
        UUID id,
        String name,
        String slug,
        String description,

        // ── İletişim ─────────────────────────────────────────────────────────
        /** Telefon numarası (opsiyonel). */
        String phone,

        /** E-posta adresi (opsiyonel). */
        String email,

        /** Web sitesi URL'si (opsiyonel). */
        String website,

        // ── Konum ────────────────────────────────────────────────────────────
        String cityName,
        String districtName,

        /** Mahalle adı (örn. "Moda"). */
        String neighborhoodName,

        /** Açık adres satırı (sokak, cadde, kapı no vb.). */
        String addressLine,

        // ── Fotoğraf / puan ──────────────────────────────────────────────────
        /** Kapak fotoğrafı URL'si; is_cover=true olan photo kaydından gelir, yoksa null. */
        String coverPhotoUrl,

        /** Görünür yorumların ortalama puanı; yorum yoksa null. */
        Double averageRating,

        /** Görünür (isVisible=true) yorum sayısı. */
        Long reviewCount,

        // ── Koordinat ve mesafe ───────────────────────────────────────────────
        Double latitude,
        Double longitude,

        /**
         * Kullanıcının konumuna olan mesafe (km).
         * Servis katmanında HaversineUtil ile doldurulur.
         */
        Double distanceKm,

        /** İşletmenin ait olduğu kategori adlarının listesi. */
        List<String> categoryNames,

        // ── İç listeler ───────────────────────────────────────────────────────
        /** Aktif hizmetlerin özet listesi (isActive=true, sortOrder ile sıralı). */
        List<ServiceSummary> services,

        /** Aktif personelin özet listesi (isActive=true, sortOrder ile sıralı). */
        List<StaffSummary> staff

) {

    // ═══════════════════════════════════════════════════════════════════════
    // İç record: ServiceSummary
    // Amacı: Hizmet listesinde gösterilen minimal hizmet bilgisi.
    // Ne yapıyor: Müşteriye randevu seçimi ekranında hizmet adı, süresi,
    //             fiyatı ve görseli sunar.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Detay sayfasında gösterilen hizmet özeti.
     */
    public record ServiceSummary(

            UUID id,

            String name,

            /** Randevu slot süresi (dakika). */
            Integer durationMin,

            BigDecimal price,

            /** Para birimi kodu (örn. "TRY"). */
            String currency,

            /** Hizmet kapak görseli URL'si; yoksa null. */
            String imageUrl

    ) {}

    // ═══════════════════════════════════════════════════════════════════════
    // İç record: StaffSummary
    // Amacı: Personel listesinde gösterilen minimal personel bilgisi.
    // Ne yapıyor: Müşteriye personel seçimi ekranında ad, unvan ve fotoğraf sunar.
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Detay sayfasında gösterilen personel özeti.
     */
    public record StaffSummary(

            UUID id,

            String fullName,

            /** Görünen unvan (örn. "Uzman Berber", "Diş Hekimi"). */
            String title,

            /** Profil fotoğrafı URL'si; yoksa null. */
            String profilePhotoUrl

    ) {}
}
