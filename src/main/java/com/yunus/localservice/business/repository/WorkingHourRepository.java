package com.yunus.localservice.business.repository;

import com.yunus.localservice.business.entity.WorkingHour;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Haftalık çalışma saatleri; işletme geneli veya personel bazlı kayıtlar.
 */
public interface WorkingHourRepository extends JpaRepository<WorkingHour, UUID> {

    List<WorkingHour> findByBusinessIdAndStaffIsNull(UUID businessId);

    List<WorkingHour> findByBusinessIdAndStaffId(UUID businessId, UUID staffId);

}
