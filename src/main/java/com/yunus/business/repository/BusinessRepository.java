package com.yunus.business.repository;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme kayıtları; sahip, slug ve onay durumu sorguları.
 */
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    List<Business> findByOwnerId(UUID ownerId);

    List<Business> findByStatus(BusinessStatus status);

}
