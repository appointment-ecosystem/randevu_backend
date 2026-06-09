package com.yunus.appointment.repository;

import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link Appointment} entity'si için Spring Data JPA repository.
 *
 * <p>Sorumluluklar:
 * <ul>
 *   <li>Slot çakışma kontrolleri — personel bazlı ve işletme bazlı (staff=null)</li>
 *   <li>Kullanıcı randevu listesi sorgular</li>
 *   <li>İşletme takvim görünümü (tarih aralığı + isteğe bağlı filtre)</li>
 *   <li>Personel meşguliyet slotları (slot hesaplama servisine girdi)</li>
 * </ul>
 *
 * <p>Çakışma önleme mimarisi: Bu katmandaki boolean kontroller yalnızca
 * yardımcı bir güvencedir. Asıl koruma katmanları şunlardır:
 * <ol>
 *   <li>Redis dağıtık slot lock (servis katmanı)</li>
 *   <li>Veritabanı unique constraint: {@code (staff_id, start_time)}</li>
 * </ol>
 */
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // -------------------------------------------------------------------------
    // 1. Slot çakışma kontrolü — personel bazlı
    // -------------------------------------------------------------------------

    /**
     * Belirli bir personel için verilen zaman aralığında PENDING veya CONFIRMED
     * durumunda çakışan randevu olup olmadığını kontrol eder.
     *
     * <p>Çakışma koşulu: iki aralık [s1,e1) ve [s2,e2) çakışır ↔ s1 < e2 AND e1 > s2.
     *
     * <p>Kullanım: {@code AppointmentService.createAppointment()} içinde Redis lock
     * alındıktan hemen sonra DB seviyesinde ikinci güvence olarak çağrılır.
     *
     * @param staffId   meşguliyeti kontrol edilecek personelin UUID'si
     * @param slotStart yeni randevunun başlangıç zamanı
     * @param slotEnd   yeni randevunun bitiş zamanı
     * @param statuses  çakışma sayılacak durum kümesi (PENDING, CONFIRMED)
     * @return çakışan randevu varsa {@code true}
     */
    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.staff.id = :staffId
              AND a.status IN :statuses
              AND a.startTime < :slotEnd
              AND a.endTime > :slotStart
            """)
    boolean existsOverlappingByStaff(
            @Param("staffId") UUID staffId,
            @Param("slotStart") OffsetDateTime slotStart,
            @Param("slotEnd") OffsetDateTime slotEnd,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    // -------------------------------------------------------------------------
    // 2. Slot çakışma kontrolü — işletme bazlı (staff = null)
    // -------------------------------------------------------------------------

    /**
     * Personel ataması olmayan (staff=null) işletme randevuları için verilen
     * zaman aralığında PENDING veya CONFIRMED durumunda çakışan kayıt olup
     * olmadığını kontrol eder.
     *
     * <p>Personel seçimini zorunlu kılmayan işletmelerde randevular doğrudan
     * işletmeye bağlanır; çakışma kontrolü de işletme kapasitesi üzerinden yapılır.
     *
     * <p>Kullanım: {@code AppointmentService.createAppointment()} içinde
     * {@code staffId == null} durumunda çağrılır.
     *
     * @param businessId meşguliyeti kontrol edilecek işletmenin UUID'si
     * @param slotStart  yeni randevunun başlangıç zamanı
     * @param slotEnd    yeni randevunun bitiş zamanı
     * @param statuses   çakışma sayılacak durum kümesi (PENDING, CONFIRMED)
     * @return çakışan randevu varsa {@code true}
     */
    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.business.id = :businessId
              AND a.staff IS NULL
              AND a.status IN :statuses
              AND a.startTime < :slotEnd
              AND a.endTime > :slotStart
            """)
    boolean existsOverlappingByBusinessWithoutStaff(
            @Param("businessId") UUID businessId,
            @Param("slotStart") OffsetDateTime slotStart,
            @Param("slotEnd") OffsetDateTime slotEnd,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    // -------------------------------------------------------------------------
    // 3. Kullanıcı randevu listesi
    // -------------------------------------------------------------------------

    /**
     * Belirli bir kullanıcıya ait tüm randevuları en yeni tarihten eskiye sıralar.
     *
     * <p>Kullanım: {@code GET /api/v1/appointments/my} — kullanıcının kendi
     * randevu geçmişini görüntülemesi.
     *
     * @param userId randevuları getirilecek kullanıcının UUID'si
     * @return kullanıcıya ait randevular, {@code startTime DESC} sıralı
     */
    List<Appointment> findByUserIdOrderByStartTimeDesc(UUID userId);

    // -------------------------------------------------------------------------
    // 4. Kullanıcının belirli statüsteki randevuları
    // -------------------------------------------------------------------------

    /**
     * Belirli bir kullanıcıya ait, verilen statüdeki randevuları en yeni tarihten
     * eskiye sıralı olarak döner.
     *
     * <p>Kullanım: {@code GET /api/v1/appointments/my?status=PENDING} gibi
     * durum filtreli kullanıcı randevu sorguları.
     *
     * @param userId kullanıcının UUID'si
     * @param status filtrelenmek istenen randevu durumu
     * @return eşleşen randevular, {@code startTime DESC} sıralı
     */
    List<Appointment> findByUserIdAndStatusOrderByStartTimeDesc(UUID userId, AppointmentStatus status);

    // -------------------------------------------------------------------------
    // 5. İşletme takvimi — tarih aralığı
    // -------------------------------------------------------------------------

    /**
     * Bir işletmenin verilen tarih aralığında başlayan tüm randevularını
     * kronolojik sırayla döner.
     *
     * <p>Kullanım: {@code GET /api/v1/business/{id}/appointments?from=...&to=...}
     * — işletme sahibinin günlük veya haftalık takvim görünümü.
     *
     * <p>Not: {@code rangeEnd} dahil değildir; yarı-açık aralık [rangeStart, rangeEnd).
     *
     * @param businessId işletmenin UUID'si
     * @param rangeStart aralığın başlangıcı (dahil)
     * @param rangeEnd   aralığın sonu (hariç)
     * @return eşleşen randevular, {@code startTime ASC} sıralı
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.business.id = :businessId
              AND a.startTime >= :rangeStart
              AND a.startTime < :rangeEnd
            ORDER BY a.startTime ASC
            """)
    List<Appointment> findByBusinessIdAndTimeRange(
            @Param("businessId") UUID businessId,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // -------------------------------------------------------------------------
    // 6. İşletme takvimi — tarih aralığı + statü filtresi
    // -------------------------------------------------------------------------

    /**
     * Bir işletmenin verilen tarih aralığında başlayan ve belirli bir statüdeki
     * randevularını kronolojik sırayla döner.
     *
     * <p>Kullanım: {@code GET /api/v1/business/{id}/appointments?from=...&to=...&status=CONFIRMED}
     * — işletme sahibinin onaylanmış/bekleyen gibi belirli statüdeki randevuları filtrelemesi.
     *
     * <p>Not: {@code rangeEnd} dahil değildir; yarı-açık aralık [rangeStart, rangeEnd).
     *
     * @param businessId işletmenin UUID'si
     * @param status     filtrelenmek istenen randevu durumu
     * @param rangeStart aralığın başlangıcı (dahil)
     * @param rangeEnd   aralığın sonu (hariç)
     * @return eşleşen randevular, {@code startTime ASC} sıralı
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.business.id = :businessId
              AND a.status = :status
              AND a.startTime >= :rangeStart
              AND a.startTime < :rangeEnd
            ORDER BY a.startTime ASC
            """)
    List<Appointment> findByBusinessIdAndStatusAndTimeRange(
            @Param("businessId") UUID businessId,
            @Param("status") AppointmentStatus status,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // -------------------------------------------------------------------------
    // 7. Personel meşguliyet slotları — slot hesaplama için
    // -------------------------------------------------------------------------

    /**
     * Belirli bir personelin verilen tarih aralığında başlayan PENDING veya
     * CONFIRMED durumundaki randevularını döner.
     *
     * <p>Kullanım: {@code SlotCalculationService} içinde personelin gün içindeki
     * meşgul aralıklarını tespit etmek ve müsait slotları hesaplamak için kullanılır.
     *
     * <p>Not: {@code rangeEnd} dahil değildir; yarı-açık aralık [rangeStart, rangeEnd).
     *
     * @param staffId    meşgul slotları getirilecek personelin UUID'si
     * @param statuses   aktif sayılacak durum kümesi (PENDING, CONFIRMED)
     * @param rangeStart aralığın başlangıcı (dahil)
     * @param rangeEnd   aralığın sonu (hariç)
     * @return personelin aktif randevuları, sırasız
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.staff.id = :staffId
              AND a.status IN :statuses
              AND a.startTime >= :rangeStart
              AND a.startTime < :rangeEnd
            """)
    List<Appointment> findActiveByStaffAndTimeRange(
            @Param("staffId") UUID staffId,
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // -------------------------------------------------------------------------
    // 8. İşletme meşguliyet slotları (staff = null) — slot hesaplama için
    // -------------------------------------------------------------------------

    /**
     * Personel ataması olmayan işletmenin verilen tarih aralığında başlayan
     * PENDING veya CONFIRMED durumundaki randevularını döner.
     *
     * <p>Kullanım: {@code SlotCalculationService} içinde, personel seçimini
     * zorunlu kılmayan işletmelerde işletme kapasitesine göre meşgul aralıkları
     * tespit etmek için kullanılır.
     *
     * <p>Not: {@code rangeEnd} dahil değildir; yarı-açık aralık [rangeStart, rangeEnd).
     *
     * @param businessId meşgul slotları getirilecek işletmenin UUID'si
     * @param statuses   aktif sayılacak durum kümesi (PENDING, CONFIRMED)
     * @param rangeStart aralığın başlangıcı (dahil)
     * @param rangeEnd   aralığın sonu (hariç)
     * @return işletmenin aktif (staff=null) randevuları, sırasız
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.business.id = :businessId
              AND a.staff IS NULL
              AND a.status IN :statuses
              AND a.startTime >= :rangeStart
              AND a.startTime < :rangeEnd
            """)
    List<Appointment> findActiveByBusinessWithoutStaffAndTimeRange(
            @Param("businessId") UUID businessId,
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // -------------------------------------------------------------------------
    // 9. Admin — kullanıcı randevuları (sayfalanmış)
    // -------------------------------------------------------------------------

    /**
     * Belirli bir kullanıcıya ait tüm randevuları başlangıç zamanına göre
     * azalan sırada sayfalanmış olarak döner.
     *
     * <p>Kullanım: Admin panelinde {@code GET /api/v1/admin/users/{id}/appointments}
     * endpoint'inde kullanıcının randevu geçmişini görüntülemek için çağrılır.
     *
     * @param userId   randevuları getirilecek kullanıcının UUID'si
     * @param pageable sayfalama ve sıralama parametreleri
     * @return sayfalanmış randevu listesi, {@code startTime DESC} sıralı
     */
    Page<Appointment> findByUserIdOrderByStartTimeDesc(UUID userId, Pageable pageable);

    // -------------------------------------------------------------------------
    // 10. Zamanlı bildirim sorguları — scheduler için
    // -------------------------------------------------------------------------

    /**
     * Verilen zaman penceresi içinde başlayacak, PENDING veya CONFIRMED durumundaki
     * randevuları döner. Hatırlatma bildirimi göndermek için kullanılır.
     *
     * <p>Kullanım: {@code NotificationScheduler.sendAppointmentReminders()} —
     * yaklaşık 24 saat sonraki randevular için hatırlatma bildirimi tetikler.
     *
     * @param windowStart pencere başlangıcı (dahil)
     * @param windowEnd   pencere sonu (hariç)
     * @return hatırlatma yapılacak randevular
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.status IN ('PENDING', 'CONFIRMED')
              AND a.startTime >= :windowStart
              AND a.startTime < :windowEnd
            """)
    List<Appointment> findAppointmentsForReminder(
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    /**
     * Verilen zaman penceresi içinde tamamlanmış (endTime bu aralıkta olan),
     * COMPLETED durumundaki randevuları döner. Yorum isteme bildirimi için kullanılır.
     *
     * <p>Kullanım: {@code NotificationScheduler.sendReviewRequests()} —
     * yaklaşık 1 saat önce tamamlanan randevular için yorum isteme bildirimi tetikler.
     *
     * @param windowStart pencere başlangıcı (dahil)
     * @param windowEnd   pencere sonu (hariç)
     * @return yorum isteme bildirimi yapılacak randevular
     */
    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.status = 'COMPLETED'
              AND a.endTime >= :windowStart
              AND a.endTime < :windowEnd
            """)
    List<Appointment> findCompletedAppointmentsForReview(
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );
}

