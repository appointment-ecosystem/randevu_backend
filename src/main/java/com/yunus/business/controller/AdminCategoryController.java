package com.yunus.business.controller;

import com.yunus.business.dto.AdminCategoryCreateRequest;
import com.yunus.business.dto.AdminCategoryResponse;
import com.yunus.business.dto.AdminCategoryUpdateRequest;
import com.yunus.business.service.AdminCategoryService;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin rolüne sahip kullanıcıların işletme kategorilerini oluşturma, güncelleme,
 * listeleme ve aktiflik yönetimi işlemlerini yapabilmesini sağlayan denetleyici sınıf.
 * Tüm endpoint'ler ROLE_ADMIN yetkisi gerektirir.
 */
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param adminCategoryService Admin kategori yönetim servisi
     */
    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    /**
     * Sistemdeki tüm kategorileri sortOrder'a göre artan sırada listeler.
     * GET /api/v1/admin/categories
     *
     * @return Kategori listesi yanıtı
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<AdminCategoryResponse>>> getAllCategories() {
        List<AdminCategoryResponse> response = adminCategoryService.getAllCategories();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Yeni bir işletme kategorisi oluşturur.
     * POST /api/v1/admin/categories
     *
     * @param request Oluşturulacak kategoriye ait bilgileri içeren istek DTO'su
     * @return HTTP 201 Created ve oluşturulan kategorinin yanıt DTO'su
     */
    @PostMapping
    public ResponseEntity<BaseResponse<AdminCategoryResponse>> createCategory(
            @Valid @RequestBody AdminCategoryCreateRequest request) {
        AdminCategoryResponse response = adminCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Mevcut bir kategoriyi günceller.
     * PUT /api/v1/admin/categories/{id}
     *
     * @param id      Güncellenecek kategorinin kimliği
     * @param request Yeni değerleri içeren güncelleme DTO'su
     * @return Güncellenmiş kategorinin yanıt DTO'su
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<AdminCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody AdminCategoryUpdateRequest request) {
        AdminCategoryResponse response = adminCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen kategoriyi pasife alır (isActive = false).
     * PATCH /api/v1/admin/categories/{id}/deactivate
     *
     * @param id Pasife alınacak kategorinin kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BaseResponse<Void>> deactivateCategory(@PathVariable UUID id) {
        adminCategoryService.deactivateCategory(id);
        return ResponseEntity.ok(BaseResponse.success("Kategori başarıyla pasife alındı"));
    }

    /**
     * Belirtilen kategoriyi aktif hale getirir (isActive = true).
     * PATCH /api/v1/admin/categories/{id}/activate
     *
     * @param id Aktifleştirilecek kategorinin kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<BaseResponse<Void>> activateCategory(@PathVariable UUID id) {
        adminCategoryService.activateCategory(id);
        return ResponseEntity.ok(BaseResponse.success("Kategori başarıyla aktifleştirildi"));
    }
}
