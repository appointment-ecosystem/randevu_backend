package com.yunus.business.dto;

import java.util.UUID;

/**
 * Herkese açık (public) kategori listeleme endpoint'i için yanıt DTO'su.
 * Yalnızca istemci tarafında ihtiyaç duyulan alanları içerir.
 */
public record CategoryResponse(

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

        /** Listeleme sıralamasında kullanılan sayısal değer (küçükten büyüğe). */
        Integer sortOrder
) {
}
