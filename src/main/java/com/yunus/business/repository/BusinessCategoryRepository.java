package com.yunus.business.repository;

import com.yunus.business.entity.BusinessCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme kategorileri; slug ile filtreleme ve admin CRUD.
 */
public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, UUID> {

    Optional<BusinessCategory> findBySlug(String slug);

}
