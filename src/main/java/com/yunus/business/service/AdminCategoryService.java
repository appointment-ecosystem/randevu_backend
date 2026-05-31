package com.yunus.business.service;

import com.yunus.business.dto.AdminCategoryCreateRequest;
import com.yunus.business.dto.AdminCategoryResponse;
import com.yunus.business.dto.AdminCategoryUpdateRequest;
import java.util.List;
import java.util.UUID;

/**
 * Admin paneli üzerinden işletme kategorilerinin oluşturma, güncelleme,
 * listeleme ve aktiflik yönetimi işlemlerini tanımlayan servis arayüzü.
 */
public interface AdminCategoryService {

    /**
     * Sistemdeki tüm kategorileri sortOrder değerine göre artan sırada listeler.
     *
     * @return Sıralı kategori listesi
     */
    List<AdminCategoryResponse> getAllCategories();

    /**
     * Yeni bir işletme kategorisi oluşturur.
     * Aynı name veya slug'a sahip kategori mevcutsa çakışma hatası fırlatır.
     *
     * @param request Oluşturulacak kategoriye ait bilgiler
     * @return Oluşturulan kategorinin yanıt DTO'su
     */
    AdminCategoryResponse createCategory(AdminCategoryCreateRequest request);

    /**
     * Mevcut bir kategoriyi günceller.
     * Kategori bulunamazsa kaynak bulunamadı hatası fırlatır.
     * Aynı name veya slug'a sahip farklı bir kategori mevcutsa çakışma hatası fırlatır.
     *
     * @param id      Güncellenecek kategorinin kimliği
     * @param request Yeni değerleri içeren güncelleme DTO'su
     * @return Güncellenmiş kategorinin yanıt DTO'su
     */
    AdminCategoryResponse updateCategory(UUID id, AdminCategoryUpdateRequest request);

    /**
     * Belirtilen kategoriyi pasife alır (isActive = false).
     * Kategori bulunamazsa kaynak bulunamadı hatası fırlatır.
     *
     * @param id Pasife alınacak kategorinin kimliği
     */
    void deactivateCategory(UUID id);

    /**
     * Belirtilen kategoriyi aktif hale getirir (isActive = true).
     * Kategori bulunamazsa kaynak bulunamadı hatası fırlatır.
     *
     * @param id Aktifleştirilecek kategorinin kimliği
     */
    void activateCategory(UUID id);
}
