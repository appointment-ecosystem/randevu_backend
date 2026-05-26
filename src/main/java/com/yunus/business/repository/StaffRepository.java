package com.yunus.business.repository;

import com.yunus.business.entity.Staff;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İşletme personeli listesi; randevu ve çalışma saati ekranlarında kullanılır.
 */
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    List<Staff> findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(UUID businessId);

}
