package com.yunus.localservice.business.repository;

import com.yunus.localservice.business.entity.Service;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletmeye ait aktif hizmet listesi (randevu ve fiyatlandırma için).
 */
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(UUID businessId);

}
