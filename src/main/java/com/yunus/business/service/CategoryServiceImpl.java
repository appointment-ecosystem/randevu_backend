package com.yunus.business.service;

import com.yunus.business.dto.CategoryResponse;
import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.repository.BusinessCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Herkese açık (public) kategori listeleme servisinin implementasyonu.
 * Yalnızca isActive = true olan kategorileri döndürür.
 */
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final BusinessCategoryRepository businessCategoryRepository;

    public CategoryServiceImpl(BusinessCategoryRepository businessCategoryRepository) {
        this.businessCategoryRepository = businessCategoryRepository;
    }

    /**
     * {@inheritDoc}
     * Aktif kategorileri sortOrder'a göre artan sırada listeler.
     */
    @Override
    public List<CategoryResponse> getActiveCategories() {
        return businessCategoryRepository
                .findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Entity → DTO dönüşümü
    private CategoryResponse toResponse(BusinessCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIconUrl(),
                category.getSortOrder()
        );
    }
}
