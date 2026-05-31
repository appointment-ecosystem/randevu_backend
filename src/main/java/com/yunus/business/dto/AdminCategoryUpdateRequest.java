package com.yunus.business.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Admin paneli üzerinden mevcut bir kategoriyi güncellemek için kullanılan istek DTO'su.
 * name ve slug zorunludur; description, iconUrl ve sortOrder isteğe bağlıdır.
 */
public record AdminCategoryUpdateRequest(

        /**
         * Kategorinin yeni görünen adı.
         * Boş veya yalnızca boşluk karakteri olamaz.
         */
        @NotBlank(message = "Kategori adı boş olamaz")
        String name,

        /**
         * Kategorinin yeni slug değeri.
         * Boş veya yalnızca boşluk karakteri olamaz.
         */
        @NotBlank(message = "Slug boş olamaz")
        String slug,

        /**
         * Kategorinin yeni açıklama metni.
         * Opsiyoneldir; null gönderilirse mevcut değer temizlenir.
         */
        String description,

        /**
         * Kategorinin yeni ikon URL'si.
         * Opsiyoneldir; null gönderilirse mevcut değer temizlenir.
         */
        String iconUrl,

        /**
         * Listeleme sıralamasındaki yeni sayısal değer.
         * Opsiyoneldir; null gönderilirse mevcut sortOrder değiştirilmez.
         */
        Integer sortOrder
) {
}
