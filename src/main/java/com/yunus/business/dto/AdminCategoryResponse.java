package com.yunus.business.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin paneli için kategori okuma (GET) yanıtı.
 * İşletme kategorisinin tüm alanlarını ve oluşturulma zamanını içerir.
 */
public record AdminCategoryResponse(

        /** Kategorinin benzersiz kimliği. */
        UUID id,

        /** Kategorinin görünen adı (örn. "Berber", "Kuaför"). */
        String name,

        /** URL ve filtre parametrelerinde kullanılan kısa tanımlayıcı (örn. "berber"). */
        String slug,

        /** Kategoriye ait açıklama metni; boş olabilir. */
        String description,

        /** Kategorinin ikonunu temsil eden URL; boş olabilir. */
        String iconUrl,

        /** Kategorinin aktif/pasif durumu. */
        Boolean isActive,

        /** Listeleme sıralamasında kullanılan sayısal değer (küçükten büyüğe). */
        Integer sortOrder,

        /** Kaydın ilk oluşturulduğu tarih ve saat. */
        OffsetDateTime createdAt
) {
}
