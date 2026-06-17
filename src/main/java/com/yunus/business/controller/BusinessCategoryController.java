package com.yunus.business.controller;

import com.yunus.business.dto.CategoryResponse;
import com.yunus.business.service.CategoryService;
import com.yunus.common.response.BaseResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Herkese açık (authentication gerektirmeyen) işletme kategorisi listeleme endpoint'i.
 * GET /api/v1/business-categories — yalnızca aktif kategorileri (isActive = true) döndürür.
 *
 * <p>SecurityConfig'de /api/v1/business-categories/** permitAll() olarak tanımlıdır.
 * İstemciler (mobil / web) işletme türü seçimi için bu endpoint'i kullanır.</p>
 */
@RestController
@RequestMapping("/api/v1/business-categories")
public class BusinessCategoryController {

    private final CategoryService categoryService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param categoryService Aktif kategori listeleme servisi
     */
    public BusinessCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Aktif kategorileri sortOrder'a göre artan sırada listeler.
     * GET /api/v1/business-categories
     *
     * @return Aktif kategori listesi; herhangi bir authentication gerektirmez.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(BaseResponse.success(categories));
    }
}
