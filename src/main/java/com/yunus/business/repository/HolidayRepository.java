package com.yunus.business.repository;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.Holiday;
import com.yunus.business.entity.Staff;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Kapalı gün / tatil kayıtları; slot üretiminde hariç tutulur.
 * Hem tarih aralıklı hem de sıralı sorgular tanımlıdır.
 */
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    // Tarih aralıklı sorgular (mevcut — korundu; slot hesaplamasında kullanılır)
    List<Holiday> findByBusinessIdAndDateBetween(UUID businessId, LocalDate start, LocalDate end);

    List<Holiday> findByBusinessIdAndStaffIdAndDateBetween(
            UUID businessId, UUID staffId, LocalDate start, LocalDate end);

    // Entity tabanlı tam liste sorgular — getHolidays metodunda kullanılır
    List<Holiday> findByBusinessAndStaffIsNullOrderByDateAsc(Business business);

    List<Holiday> findByBusinessAndStaffOrderByDateAsc(Business business, Staff staff);

    // Tatil çakışma kontrolü — addHoliday'de aynı gün aynı kapsam tekrarını önler
    boolean existsByBusinessAndStaffAndDate(Business business, Staff staff, LocalDate date);

    // Slot hesaplamasında tek gün tatil kontrolü — işletme geneli (staff = null)
    java.util.Optional<Holiday> findByBusinessIdAndStaffIsNullAndDate(UUID businessId, LocalDate date);

    // Slot hesaplamasında tek gün tatil kontrolü — personel bazlı
    java.util.Optional<Holiday> findByBusinessIdAndStaffIdAndDate(UUID businessId, UUID staffId, LocalDate date);
}
