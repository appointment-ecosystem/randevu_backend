package com.yunus.business.repository;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.entity.WorkingHour;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Haftalık çalışma saatleri; işletme geneli veya personel bazlı kayıtlar.
 * İki sorgu türü var: ID tabanlı (basit) ve entity tabanlı (dayOfWeek sıralı).
 */
public interface WorkingHourRepository extends JpaRepository<WorkingHour, UUID> {

    // ID tabanlı sorgular (mevcut — korundu)
    List<WorkingHour> findByBusinessIdAndStaffIsNull(UUID businessId);

    List<WorkingHour> findByBusinessIdAndStaffId(UUID businessId, UUID staffId);

    // Entity tabanlı sıralı sorgular — setBusinessHours / setStaffHours silme adımında kullanılır
    List<WorkingHour> findByBusinessAndStaffIsNullOrderByDayOfWeekAsc(Business business);

    List<WorkingHour> findByBusinessAndStaffOrderByDayOfWeekAsc(Business business, Staff staff);
}
