package com.yunus.localservice.business.repository;

import com.yunus.localservice.business.entity.BusinessPhoto;
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
