package com.yunus.business.repository;

import com.yunus.business.entity.BusinessPhoto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme galeri fotoğrafları; sıralama ve kapak görseli sorguları.
 */
public interface BusinessPhotoRepository extends JpaRepository<BusinessPhoto, UUID> {

    List<BusinessPhoto> findByBusinessIdOrderBySortOrderAsc(UUID businessId);

    Optional<BusinessPhoto> findByBusinessIdAndIsCoverTrue(UUID businessId);

}
