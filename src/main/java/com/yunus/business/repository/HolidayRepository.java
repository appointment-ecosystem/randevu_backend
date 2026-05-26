package com.yunus.business.repository;

import com.yunus.business.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kapalı gün / tatil kayıtları; slot üretiminde hariç tutulur.
 */
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    List<Holiday> findByBusinessIdAndDateBetween(UUID businessId, LocalDate start, LocalDate end);

    List<Holiday> findByBusinessIdAndStaffIdAndDateBetween(
            UUID businessId, UUID staffId, LocalDate start, LocalDate end);

}
