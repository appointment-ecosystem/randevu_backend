package com.yunus.localservice.appointment.repository;

import com.yunus.localservice.appointment.entity.Appointment;
import com.yunus.localservice.appointment.entity.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Randevu sorguları — takvim, kullanıcı listesi ve slot çakışma kontrolleri.
 * Çakışma önleme tek başına bu katmana bırakılmaz; Redis lock + DB constraint birlikte kullanılır.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByUserIdOrderByStartTimeDesc(UUID userId);

    // İşletme takvim görünümü (günlük/haftalık aralık)
    List<Appointment> findByBusinessIdAndStartTimeBetween(
            UUID businessId,
            OffsetDateTime start,
            OffsetDateTime end
    );

    // Personel bazlı takvim
    List<Appointment> findByStaffIdAndStartTimeBetween(
            UUID staffId,
            OffsetDateTime start,
            OffsetDateTime end
    );

    // Aynı personele aynı saatte aktif randevu var mı (yardımcı kontrol; asıl koruma DB unique index)
    boolean existsByStaffIdAndStartTimeAndStatusIn(
            UUID staffId,
            OffsetDateTime startTime,
            Collection<AppointmentStatus> statuses
    );

    List<Appointment> findByBusinessIdAndStatus(UUID businessId, AppointmentStatus status);

}
