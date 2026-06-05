package com.yunus.business.service;

import com.yunus.business.dto.CategoryResponse;
import java.util.List;

/**
 * Herkese açık (public) kategori listeleme işlemlerini tanımlayan servis arayüzü.
 * Auth gerektirmeyen endpoint'lerden çağrılır.
 */
public interface CategoryService {

    /**
     * Aktif olan tüm kategorileri (isActive = true) sortOrder değerine
     * göre artan sırada listeler.
     *
     * @return Aktif kategori listesi
     */
    List<CategoryResponse> getActiveCategories();
}
