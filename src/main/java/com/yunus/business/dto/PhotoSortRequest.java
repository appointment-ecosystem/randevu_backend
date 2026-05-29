package com.yunus.business.dto;

import java.util.UUID;

/**
 * Fotoğraf sıralama güncellemesi için istek kaydı.
 * Her fotoğrafın yeni sortOrder değerini taşır.
 */
public record PhotoSortRequest(
        UUID photoId,
        Integer sortOrder
) {
}
