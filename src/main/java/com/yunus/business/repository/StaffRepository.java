package com.yunus.business.repository;

import com.yunus.business.entity.Staff;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme personeli listesi; randevu ve çalışma saati ekranlarında kullanılır.
 */
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    List<Staff> findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(UUID businessId);

    // Slot ve randevu servislerinde tekil personel doğrulaması
    Optional<Staff> findByIdAndBusinessIdAndIsActiveTrue(UUID id, UUID businessId);

    // Personel seçimi olmayan sorgulamalar için tüm aktif personel
    List<Staff> findByBusinessIdAndIsActiveTrue(UUID businessId);

    // BUSINESS_EMPLOYEE kullanıcısının bağlı olduğu personel kaydını bulmak için
    Optional<Staff> findByUserIdAndIsActiveTrue(UUID userId);

}
