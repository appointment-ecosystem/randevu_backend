package com.yunus.business.repository;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme kayıtları; sahip, slug ve onay durumu sorguları.
 */
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    List<Business> findByOwnerId(UUID ownerId);

    List<Business> findByStatus(BusinessStatus status);

    /**
     * Belirtilen onay durumundaki işletmeleri sayfalanmış olarak filtreler.
     * Admin panelinde işletmeleri durumlarına göre listelerken kullanılır.
     *
     * @param status Filtrelenecek işletme onay durumu
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Durumu eşleşen sayfalanmış işletme listesi
     */
    Page<Business> findByStatus(BusinessStatus status, Pageable pageable);

    // Randevu ve yetki kontrollerinde sahip doğrulaması; pasif işletmeler hariç
    Optional<Business> findByIdAndOwnerIdAndIsActiveTrue(UUID id, UUID ownerId);

}
