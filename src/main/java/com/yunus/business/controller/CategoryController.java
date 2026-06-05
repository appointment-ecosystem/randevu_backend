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
 * Herkese açık (authentication gerektirmeyen) kategori listeleme endpoint'i.
 * GET /api/v1/categories — yalnızca aktif kategorileri (isActive = true) döndürür.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param categoryService Aktif kategori listeleme servisi
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Aktif kategorileri sortOrder'a göre artan sırada listeler.
     * GET /api/v1/categories
     *
     * @return Aktif kategori listesi; herhangi bir authentication gerektirmez.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<CategoryResponse>>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(BaseResponse.success(categories));
    }
}
