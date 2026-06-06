package com.yunus.appointment.service;

import com.yunus.appointment.dto.AvailableSlotsRequest;
import com.yunus.appointment.dto.SlotResponse;
import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.business.entity.Service;
import com.yunus.business.entity.Staff;
import com.yunus.business.entity.WorkingHour;
import com.yunus.business.repository.HolidayRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.business.repository.WorkingHourRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SlotService} implementasyonu.
 *
 * <p>Algoritma özeti:
 * <ol>
 *   <li>Hizmet varlığı ve süresi doğrulanır.</li>
 *   <li>{@code staffId} null ise işletme geneli; değilse personel bazlı akış çalışır.</li>
 *   <li>Tatil ve kapalı gün kontrolleri yapılır; tatilse boş liste döner.</li>
 *   <li>Çalışma saati önce personele özel, bulunamazsa işletme geneline bakılır.</li>
 *   <li>Gün içindeki meşgul randevular DB'den alınır.</li>
 *   <li>{@link #calculateSlots} ile slotlar üretilir; çakışanlar dolu işaretlenir.</li>
 * </ol>
 *
 * <p>Timezone notu: Tüm {@link OffsetDateTime} dönüşümleri İstanbul zaman dilimiyle
 * ({@code Europe/Istanbul}) yapılır. Bu sınıftaki tüm dönüşümler
 * {@link #ISTANBUL} sabiti üzerinden geçer.
 */

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private static final Logger log = LoggerFactory.getLogger(SlotServiceImpl.class);

    /** Tüm timezone dönüşümlerinde kullanılan İstanbul zaman dilimi. */
    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    /** PENDING veya CONFIRMED durumdaki randevular meşgul kabul edilir. */
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final WorkingHourRepository workingHourRepository;
    private final HolidayRepository holidayRepository;
    private final AppointmentRepository appointmentRepository;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Staffsiz (işletme bazlı) ve staff'lı (personel bazlı) olmak üzere iki
     * ana akış bulunur. Her ikisinde de tatil, kapalı gün ve meşgul randevu
     * kontrolleri yapılarak {@link SlotResponse} listesi üretilir.
     */
    @Override
    public List<SlotResponse> getAvailableSlots(AvailableSlotsRequest request) {
        UUID businessId = request.businessId();
        UUID serviceId  = request.serviceId();
        UUID staffId    = request.staffId();   // nullable
        LocalDate date  = request.date();

        // ------------------------------------------------------------------
        // Adım 1 — Hizmet kontrolü; hizmet aktif ve bu işletmeye ait olmalı
        // ------------------------------------------------------------------
        Service service = serviceRepository
                .findByIdAndBusinessIdAndIsActiveTrue(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hizmet bulunamadı veya bu işletmeye ait değil: " + serviceId));
        Integer durationMin = service.getDurationMin();
        if (durationMin == null || durationMin <= 0) {
            throw new BusinessException("Hizmet süresi geçersiz veya tanımlanmamış.");
        }

        // ------------------------------------------------------------------
        // Adım 2 — Haftanın günü (1=Pazartesi, 7=Pazar)
        // ------------------------------------------------------------------
        int dayOfWeek = date.getDayOfWeek().getValue();
        log.debug("[SlotService] date={} dayOfWeek={} ({}), businessId={}, serviceId={}, staffId={}, durationMin={}",
                date, dayOfWeek, date.getDayOfWeek(), businessId, serviceId, staffId, durationMin);

        // ------------------------------------------------------------------
        // Adım 3 — staffId durumuna göre akış ayrılır
        // ------------------------------------------------------------------
        if (staffId == null) {
            return resolveBusinessSlots(businessId, date, dayOfWeek, durationMin);
        } else {
            return resolveStaffSlots(businessId, serviceId, staffId, date, dayOfWeek, durationMin);
        }
    }

    // =========================================================================
    // Private — işletme bazlı akış (staff = null)
    // =========================================================================

    /**
     * Personel seçimi olmayan işletmeler için slot listesi üretir.
     *
     * @param businessId  işletmenin UUID'si
     * @param date        sorgu tarihi
     * @param dayOfWeek   1–7 (Pazartesi–Pazar)
     * @param durationMin hizmet süresi (dakika)
     * @return slot listesi; tatil veya kapalı günde boş liste
     */
    private List<SlotResponse> resolveBusinessSlots(
            UUID businessId, LocalDate date, int dayOfWeek, int durationMin) {

        // 3a — İşletme tatil kontrolü
        boolean isHoliday = holidayRepository
                .findByBusinessIdAndStaffIsNullAndDate(businessId, date)
                .isPresent();
        if (isHoliday) {
            log.warn("[SlotService] businessId={} date={} -> TAT\u0130L günü, boş liste döndü.", businessId, date);
            return Collections.emptyList();
        }

        // 3b — İşletme geneli çalışma saati
        List<WorkingHour> allWh = workingHourRepository.findByBusinessIdAndStaffIsNull(businessId);
        log.debug("[SlotService] businessId={} için {} adet working_hours kaydı bulundu. Aranan dayOfWeek={}.",
                businessId, allWh.size(), dayOfWeek);
        allWh.forEach(w -> log.debug("  -> wh: dayOfWeek={}, isClosed={}, open={}, close={}",
                w.getDayOfWeek(), w.getIsClosed(), w.getOpenTime(), w.getCloseTime()));

        WorkingHour wh = allWh.stream()
                .filter(w -> w.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);

        if (wh == null) {
            log.warn("[SlotService] businessId={} date={} dayOfWeek={} -> Eşleşen working_hours kaydı YOK, boş liste döndü.",
                    businessId, date, dayOfWeek);
            return Collections.emptyList();
        }
        if (Boolean.TRUE.equals(wh.getIsClosed())) {
            log.warn("[SlotService] businessId={} date={} dayOfWeek={} -> is_closed=true, boş liste döndü.",
                    businessId, date, dayOfWeek);
            return Collections.emptyList();
        }

        LocalTime openTime  = wh.getOpenTime();
        LocalTime closeTime = wh.getCloseTime();
        log.debug("[SlotService] openTime={}, closeTime={}, durationMin={}", openTime, closeTime, durationMin);

        if (openTime == null || closeTime == null) {
            log.warn("[SlotService] openTime veya closeTime null -> boş liste döndü.");
            return Collections.emptyList();
        }

        // 3c — Gün içindeki meşgul randevuları al (İstanbul timezone'a çevirerek)
        OffsetDateTime rangeStart = date.atTime(openTime).atZone(ISTANBUL).toOffsetDateTime();
        OffsetDateTime rangeEnd   = date.atTime(closeTime).atZone(ISTANBUL).toOffsetDateTime();

        List<Appointment> busyAppointments = appointmentRepository
                .findActiveByBusinessWithoutStaffAndTimeRange(
                        businessId, ACTIVE_STATUSES, rangeStart, rangeEnd);
        log.debug("[SlotService] Meşgul randevu sayısı: {}", busyAppointments.size());

        // 3d — Slotları hesapla (staffId = null, staffName = null)
        return calculateSlots(openTime, closeTime, date, durationMin, busyAppointments, null, null);
    }

    // =========================================================================
    // Private — personel bazlı akış
    // =========================================================================

    /**
     * Belirli bir personel için slot listesi üretir.
     *
     * <p>Çalışma saati önceliği: personele özel kayıt varsa ve açıksa kullanılır;
     * yoksa işletme geneli saate düşülür.
     *
     * @param businessId  işletmenin UUID'si
     * @param serviceId   hizmetin UUID'si (personelin bu hizmeti verip vermediği kontrolü için)
     * @param staffId     personelin UUID'si
     * @param date        sorgu tarihi
     * @param dayOfWeek   1–7 (Pazartesi–Pazar)
     * @param durationMin hizmet süresi (dakika)
     * @return slot listesi; tatil, kapalı gün veya yetenek eksikliğinde boş liste ya da exception
     */
    private List<SlotResponse> resolveStaffSlots(
            UUID businessId, UUID serviceId, UUID staffId,
            LocalDate date, int dayOfWeek, int durationMin) {

        // 3a — Personel doğrulaması; bu işletmede aktif mi?
        Staff staff = staffRepository
                .findByIdAndBusinessIdAndIsActiveTrue(staffId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Personel bulunamadı veya bu işletmeye ait değil: " + staffId));

        // Personelin bu hizmeti verebiliyor mu?
        boolean canProvideService = staff.getServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));
        if (!canProvideService) {
            throw new BusinessException("Bu personel seçilen hizmeti vermiyor.");
        }

        // 3b — Personel tatil kontrolü
        boolean staffHoliday = holidayRepository
                .findByBusinessIdAndStaffIdAndDate(businessId, staffId, date)
                .isPresent();
        if (staffHoliday) {
            return Collections.emptyList();
        }

        // İşletme geneli tatil kontrolü da yapılır
        boolean businessHoliday = holidayRepository
                .findByBusinessIdAndStaffIsNullAndDate(businessId, date)
                .isPresent();
        if (businessHoliday) {
            return Collections.emptyList();
        }

        // 3c — Çalışma saatini belirle: önce personele özel, sonra işletme geneli
        LocalTime openTime;
        LocalTime closeTime;

        WorkingHour staffWh = workingHourRepository
                .findByBusinessIdAndStaffId(businessId, staffId)
                .stream()
                .filter(w -> w.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);

        if (staffWh != null && Boolean.FALSE.equals(staffWh.getIsClosed())) {
            // Personele özel açık kayıt var — bunu kullan
            openTime  = staffWh.getOpenTime();
            closeTime = staffWh.getCloseTime();
        } else {
            // Personele özel kayıt yok ya da kapalı → işletme geneline düş
            WorkingHour businessWh = workingHourRepository
                    .findByBusinessIdAndStaffIsNull(businessId)
                    .stream()
                    .filter(w -> w.getDayOfWeek().equals(dayOfWeek))
                    .findFirst()
                    .orElse(null);

            if (businessWh == null || Boolean.TRUE.equals(businessWh.getIsClosed())) {
                // İşletme de o gün kapalı
                return Collections.emptyList();
            }
            openTime  = businessWh.getOpenTime();
            closeTime = businessWh.getCloseTime();
        }

        if (openTime == null || closeTime == null) {
            return Collections.emptyList();
        }

        // 3d — Gün içindeki meşgul randevuları al
        OffsetDateTime rangeStart = date.atTime(openTime).atZone(ISTANBUL).toOffsetDateTime();
        OffsetDateTime rangeEnd   = date.atTime(closeTime).atZone(ISTANBUL).toOffsetDateTime();

        List<Appointment> busyAppointments = appointmentRepository
                .findActiveByStaffAndTimeRange(staffId, ACTIVE_STATUSES, rangeStart, rangeEnd);

        // 3e — Slotları hesapla
        return calculateSlots(
                openTime, closeTime, date, durationMin,
                busyAppointments, staffId, staff.getFullName());
    }

    // =========================================================================
    // Private — slot üretimi
    // =========================================================================

    /**
     * Çalışma saati aralığında {@code durationMin} uzunluğunda ardışık slotlar üretir
     * ve her birinin meşgul olup olmadığını işaretler.
     *
     * <p>Çakışma koşulu: mevcut bir randevu ile yeni slot aralığı kesişiyorsa slot meşgul sayılır.
     * Matematiksel form: {@code busy.startTime < slotEnd AND busy.endTime > slotStart}
     *
     * <p>Son slot: {@code slotEnd <= workEnd} koşuluyla sınırlandırılır; kapanış saatini aşan
     * yarım kalan slotlar listeye eklenmez.
     *
     * @param openTime        günlük açılış saati (yerel, timezone'suz)
     * @param closeTime       günlük kapanış saati (yerel, timezone'suz)
     * @param date            sorgu tarihi
     * @param durationMin     her slotun süresi (dakika)
     * @param busyAppointments bu gün bu kapsam için aktif randevular
     * @param staffId         personel UUID'si; işletme bazlı ise {@code null}
     * @param staffName       personel görünen adı; işletme bazlı ise {@code null}
     * @return hesaplanmış slot listesi
     */
    private List<SlotResponse> calculateSlots(
            LocalTime openTime, LocalTime closeTime, LocalDate date,
            int durationMin, List<Appointment> busyAppointments,
            UUID staffId, String staffName) {

        // Kapanış zamanını İstanbul timezone ile OffsetDateTime'a çevir
        OffsetDateTime workEnd = date.atTime(closeTime).atZone(ISTANBUL).toOffsetDateTime();

        // İlk slotun başlangıcı
        OffsetDateTime slotStart = date.atTime(openTime).atZone(ISTANBUL).toOffsetDateTime();
        OffsetDateTime slotEnd   = slotStart.plusMinutes(durationMin);

        log.debug("[SlotService] calculateSlots: workEnd={}, ilk slotStart={}, ilk slotEnd={}",
                workEnd, slotStart, slotEnd);

        if (slotEnd.isAfter(workEnd)) {
            log.warn("[SlotService] calculateSlots: İlk slotEnd ({}) zaten workEnd ({}) sonrasında — döngüye girilmiyor! "
                    + "durationMin={} büyük ihtimalle closeTime-openTime'dan daha uzun.",
                    slotEnd, workEnd, durationMin);
        }

        List<SlotResponse> result = new ArrayList<>();

        // slotEnd kapanış saatini geçmediği sürece üret
        while (!slotEnd.isAfter(workEnd)) {

            // Bu slot meşgul mu? — herhangi bir aktif randevu ile çakışıyor mu?
            final OffsetDateTime currentSlotStart = slotStart;
            final OffsetDateTime currentSlotEnd   = slotEnd;

            boolean isBusy = busyAppointments.stream().anyMatch(busy ->
                    busy.getStartTime().isBefore(currentSlotEnd)
                    && busy.getEndTime().isAfter(currentSlotStart));

            log.debug("[SlotService] slot [{} - {}] isBusy={}", currentSlotStart, currentSlotEnd, isBusy);

            // Müsait slotları listeye ekle; meşgul slotlar dahil edilmiyor
            // (sadece müsait slotları dön — available = true garantili)
            if (!isBusy) {
                result.add(new SlotResponse(staffId, staffName, slotStart, slotEnd, true));
            }

            // Bir sonraki slot aralığına geç
            slotStart = slotStart.plusMinutes(durationMin);
            slotEnd   = slotEnd.plusMinutes(durationMin);
        }

        log.debug("[SlotService] Toplam üretilen slot sayısı: {}", result.size());
        return result;
    }
}
