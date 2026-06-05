package com.yunus.business.repository;

import com.yunus.business.entity.BusinessCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme kategorileri; slug ile filtreleme ve admin CRUD.
 */
public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, UUID> {

    /** Slug'a göre kategori sorgular. */
    Optional<BusinessCategory> findBySlug(String slug);

    /** Name'e göre kategori sorgular. */
    Optional<BusinessCategory> findByName(String name);

    /**
     * Mevcut id haricinde aynı name olan başka bir kategori var mı kontrol eder.
     * Güncelleme sırasında kendi kaydını çakışma olarak saymaması için kullanılır.
     */
    boolean existsByNameAndIdNot(String name, UUID id);

    /**
     * Mevcut id haricinde aynı slug olan başka bir kategori var mı kontrol eder.
     * Güncelleme sırasında kendi kaydını çakışma olarak saymaması için kullanılır.
     */
    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** Tüm kategorileri sıralama değerine (sortOrder) göre artan sırada getirir. */
    List<BusinessCategory> findAllByOrderBySortOrderAsc();

    /** Yalnızca aktif (isActive = true) kategorileri sortOrder'a göre artan sırada getirir. */
    List<BusinessCategory> findAllByIsActiveTrueOrderBySortOrderAsc();

}

