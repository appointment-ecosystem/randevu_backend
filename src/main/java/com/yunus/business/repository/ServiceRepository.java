package com.yunus.business.repository;

import com.yunus.business.entity.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletmeye ait aktif hizmet listesi (randevu ve fiyatlandırma için).
 */
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    List<Service> findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(UUID businessId);

    // Slot ve randevu servislerinde tekil hizmet doğrulaması
    Optional<Service> findByIdAndBusinessIdAndIsActiveTrue(UUID id, UUID businessId);

}
