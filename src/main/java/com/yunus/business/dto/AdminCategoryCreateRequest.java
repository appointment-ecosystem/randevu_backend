package com.yunus.business.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin paneli üzerinden yeni kategori oluşturmak için kullanılan istek DTO'su.
 * name ve slug zorunludur; description, iconUrl ve sortOrder isteğe bağlıdır.
 */
public record AdminCategoryCreateRequest(

        /**
         * Kategorinin görünen adı (örn. "Berber").
         * Boş veya yalnızca boşluk karakteri olamaz.
         */
        @NotBlank(message = "Kategori adı boş olamaz")
        String name,

        /**
         * URL ve sorgu parametrelerinde kullanılan kısa tanımlayıcı (örn. "berber").
         * Boş veya yalnızca boşluk karakteri olamaz.
         */
        @NotBlank(message = "Slug boş olamaz")
        String slug,

        /**
         * Kategoriye ait açıklama metni.
         * Opsiyoneldir; boş bırakılabilir.
         */
        String description,

        /**
         * Kategorinin ikonunu temsil eden URL.
         * Opsiyoneldir; boş bırakılabilir.
         */
        String iconUrl,

        /**
         * Listeleme sıralamasında kullanılan sayısal değer.
         * Belirtilmezse 0 olarak kaydedilir.
         */
        Integer sortOrder
) {
    /**
     * Compact constructor: sortOrder null ise varsayılan olarak 0 atar.
     */
    public AdminCategoryCreateRequest {
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
