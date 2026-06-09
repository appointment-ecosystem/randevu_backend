package com.yunus.appointment.service;

import com.yunus.appointment.dto.AppointmentResponse;
import com.yunus.appointment.dto.CreateAppointmentRequest;
import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.yunus.notification.dto.PushNotificationPayload;
import com.yunus.notification.entity.NotificationType;
import com.yunus.notification.service.NotificationService;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AppointmentService} implementasyonu.
 *
 * <p>Randevu oluşturma akışı:
 * <ol>
 *   <li>Hizmet, işletme ve personel doğrulaması</li>
 *   <li>Redis slot lock (dağıtık kilitleme)</li>
 *   <li>DB çakışma kontrolü (ikinci güvence)</li>
 *   <li>Kayıt — PENDING statüsünde</li>
 *   <li>Lock kasıtlı bırakılmaz; 5 dk TTL ile expire olur</li>
 * </ol>
 *
 * <p>Durum geçiş matrisi:
 * <pre>
 *   PENDING  → CONFIRMED          (confirmAppointment)
 *   PENDING  → CANCELLED_BY_USER  (cancelAppointmentByUser)
 *   PENDING  → CANCELLED_BY_BUSINESS (cancelAppointmentByBusiness)
 *   CONFIRMED → CANCELLED_BY_USER  (cancelAppointmentByUser)
 *   CONFIRMED → CANCELLED_BY_BUSINESS
 *   CONFIRMED → COMPLETED          (completeAppointment)
 *   CONFIRMED → NO_SHOW            (markNoShow)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    /** PENDING veya CONFIRMED durumdaki randevular meşgul/aktif kabul edilir. */
    private static final List<AppointmentStatus> ACTIVE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final BusinessRepository    businessRepository;
    private final ServiceRepository     serviceRepository;
    private final StaffRepository       staffRepository;
    private final SlotLockService       slotLockService;
    private final CurrentUserService    currentUserService;
    private final NotificationService   notificationService;

    // =========================================================================
    // Randevu oluşturma
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Slot lock alındıktan sonra DB çakışması varsa lock serbest bırakılır ve
     * {@link BusinessException} fırlatılır. Başarılı kayıt sonrası lock kasıtlı
     * bırakılmaz — randevu PENDING'de 5 dk TTL sonrası lock kendiliğinden düşer.
     */
    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        // Adım 1 — Giriş yapmış kullanıcı
        User user = currentUserService.getCurrentUser();
        UUID userId = user.getId();

        UUID businessId = request.businessId();
        UUID serviceId  = request.serviceId();
        UUID staffId    = request.staffId();   // nullable
        OffsetDateTime startTime = request.startTime();

        // Adım 2 — İşletme kontrolü
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "İşletme bulunamadı: " + businessId));

        // Adım 3 — Hizmet kontrolü; aktif ve bu işletmeye ait olmalı
        com.yunus.business.entity.Service service = serviceRepository
                .findByIdAndBusinessIdAndIsActiveTrue(serviceId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hizmet bulunamadı veya bu işletmeye ait değil: " + serviceId));

        // Adım 4 — Bitiş zamanı hesapla
        OffsetDateTime endTime = startTime.plusMinutes(service.getDurationMin());

        // Adım 5 — Personel kontrolü (staffId verilmişse)
        Staff staff = null;
        if (staffId != null) {
            staff = staffRepository
                    .findByIdAndBusinessIdAndIsActiveTrue(staffId, businessId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Personel bulunamadı veya bu işletmeye ait değil: " + staffId));
        }

        // Adım 6 — Redis slot lock
        boolean locked = slotLockService.tryLock(businessId, staffId, startTime, userId);
        if (!locked) {
            throw new BusinessException("Bu slot şu an başka biri tarafından seçiliyor. Lütfen tekrar deneyin.");
        }

        // Adım 7 — DB çakışma kontrolü (lock alındıktan sonra ikinci güvence)
        boolean hasConflict;
        if (staffId != null) {
            hasConflict = appointmentRepository.existsOverlappingByStaff(
                    staffId, startTime, endTime, ACTIVE_STATUSES);
        } else {
            hasConflict = appointmentRepository.existsOverlappingByBusinessWithoutStaff(
                    businessId, startTime, endTime, ACTIVE_STATUSES);
        }

        if (hasConflict) {
            // Lock'u serbest bırak; slot gerçekten dolu
            slotLockService.releaseLock(businessId, staffId, startTime, userId);
            throw new BusinessException("Bu slot dolu. Lütfen başka bir zaman dilimi seçin.");
        }

        // Adım 8 — Appointment oluştur ve kaydet
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setBusiness(business);
        appointment.setService(service);
        appointment.setStaff(staff);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setPriceSnapshot(service.getPrice());
        appointment.setCurrency(service.getCurrency());
        appointment.setNotes(request.notes());

        Appointment saved = appointmentRepository.save(appointment);

        // Adım 9 — Lock KASITLI OLARAK BIRAKILMIYOR
        // Randevu PENDING'de; lock 5 dk TTL ile expire olur.
        // Bu süre içinde başka kimse aynı slotu alamaz — ödeme/onay akışı tamamlanmalı.

        try {
            notificationService.sendToUser(saved.getUser().getId(), PushNotificationPayload.builder()
                    .title("Randevunuz Oluşturuldu ✓")
                    .body(saved.getBusiness().getName() + " · " + saved.getService().getName() + " | " + formatDateTime(saved.getStartTime()))
                    .type(NotificationType.APPOINTMENT_CREATED)
                    .data(Map.of("appointmentId", saved.getId().toString(), "screen", "appointment_detail"))
                    .build());
        } catch (Exception e) {
            log.warn("Kullanıcıya randevu oluşturma bildirimi gönderilemedi: {}", e.getMessage());
        }

        try {
            notificationService.sendToUser(saved.getBusiness().getOwner().getId(), PushNotificationPayload.builder()
                    .title("Yeni Randevu \uD83D\uDCC5")
                    .body(saved.getUser().getFullName() + " · " + saved.getService().getName() + " için randevu oluşturdu | " + formatDateTime(saved.getStartTime()))
                    .type(NotificationType.APPOINTMENT_CREATED)
                    .data(Map.of("appointmentId", saved.getId().toString(), "screen", "business_appointment_detail"))
                    .build());
        } catch (Exception e) {
            log.warn("İşletme sahibine yeni randevu bildirimi gönderilemedi: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    // =========================================================================
    // Randevu sorgulama
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Yetki: randevunun sahibi (user) veya randevunun ait olduğu işletmenin
     * sahibi (business.owner) görüntüleyebilir.
     */
    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        UUID currentUserId = currentUserService.getCurrentUserId();

        boolean isOwner          = appointment.getUser().getId().equals(currentUserId);
        boolean isBusinessOwner  = appointment.getBusiness().getOwner().getId().equals(currentUserId);

        if (!isOwner && !isBusinessOwner) {
            throw new ForbiddenException("Bu randevuyu görüntüleme yetkiniz yok.");
        }
        return toResponse(appointment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments() {
        UUID userId = currentUserService.getCurrentUserId();
        return appointmentRepository.findByUserIdOrderByStartTimeDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(AppointmentStatus status) {
        UUID userId = currentUserService.getCurrentUserId();
        return appointmentRepository.findByUserIdAndStatusOrderByStartTimeDesc(userId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUserAppointments(UUID userId) {
        // İlgili kullanıcının randevularını getir
        return appointmentRepository.findByUserIdOrderByStartTimeDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Yetki: yalnızca işletme sahibi erişebilir.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getBusinessAppointments(
            UUID businessId, OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {

        requireBusinessOwnership(businessId);
        return appointmentRepository.findByBusinessIdAndTimeRange(businessId, rangeStart, rangeEnd)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Yetki: yalnızca işletme sahibi erişebilir.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getBusinessAppointments(
            UUID businessId, AppointmentStatus status,
            OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {

        requireBusinessOwnership(businessId);
        return appointmentRepository
                .findByBusinessIdAndStatusAndTimeRange(businessId, status, rangeStart, rangeEnd)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================================
    // Durum geçişleri
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>İzin verilen kaynak durumu: {@code PENDING}
     */
    @Override
    @Transactional
    public AppointmentResponse confirmAppointment(UUID appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        // Yetki: yalnızca işletme sahibi onaylayabilir
        requireBusinessOwnershipForAppointment(appointment);

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Sadece PENDING durumundaki randevu onaylanabilir.");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        try {
            notificationService.sendToUser(saved.getUser().getId(), PushNotificationPayload.builder()
                    .title("Randevunuz Onaylandı ✓")
                    .body(saved.getBusiness().getName() + " randevunuzu onayladı | " + formatDateTime(saved.getStartTime()))
                    .type(NotificationType.APPOINTMENT_CONFIRMED)
                    .data(Map.of("appointmentId", saved.getId().toString(), "screen", "appointment_detail"))
                    .build());
        } catch (Exception e) {
            log.warn("Kullanıcıya randevu onay bildirimi gönderilemedi: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>İzin verilen kaynak durumlar: {@code PENDING}, {@code CONFIRMED}
     * <br>Lock varsa serbest bırakılır; yeni bir randevu için slot açılır.
     */
    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentByUser(UUID appointmentId, String reason) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        // Yetki: yalnızca randevunun sahibi iptal edebilir
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (!appointment.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("Bu randevuyu iptal etme yetkiniz yok.");
        }

        AppointmentStatus currentStatus = appointment.getStatus();
        if (currentStatus != AppointmentStatus.PENDING && currentStatus != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Bu randevu iptal edilemez (mevcut durum: " + currentStatus + ").");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED_BY_USER);
        appointment.setCancellationReason(reason);

        // Redis lock varsa serbest bırak; slot tekrar müsait hale gelir
        UUID staffId    = appointment.getStaff() != null ? appointment.getStaff().getId() : null;
        UUID businessId = appointment.getBusiness().getId();
        slotLockService.releaseLock(businessId, staffId, appointment.getStartTime(), currentUserId);

        Appointment saved = appointmentRepository.save(appointment);

        try {
            notificationService.sendToUser(saved.getBusiness().getOwner().getId(), PushNotificationPayload.builder()
                    .title("Randevu İptal Edildi")
                    .body(saved.getUser().getFullName() + " randevusunu iptal etti | " + formatDateTime(saved.getStartTime()))
                    .type(NotificationType.APPOINTMENT_CANCELLED)
                    .data(Map.of("appointmentId", saved.getId().toString(), "screen", "business_appointment_detail"))
                    .build());
        } catch (Exception e) {
            log.warn("İşletme sahibine randevu iptal bildirimi gönderilemedi: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>İzin verilen kaynak durumlar: {@code PENDING}, {@code CONFIRMED}
     * <br>Lock serbest bırakılırken randevunun orijinal userId kullanılır
     * (lock'u işletme sahibi değil kullanıcı almıştı).
     */
    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentByBusiness(UUID appointmentId, String reason) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        // Yetki: yalnızca işletme sahibi iptal edebilir
        requireBusinessOwnershipForAppointment(appointment);

        AppointmentStatus currentStatus = appointment.getStatus();
        if (currentStatus != AppointmentStatus.PENDING && currentStatus != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Bu randevu iptal edilemez (mevcut durum: " + currentStatus + ").");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED_BY_BUSINESS);
        appointment.setCancellationReason(reason);

        // Lock'u randevunun sahibi userId ile bırak (lock'u o kullanıcı almıştı)
        UUID staffId       = appointment.getStaff() != null ? appointment.getStaff().getId() : null;
        UUID businessId    = appointment.getBusiness().getId();
        UUID appointmentUserId = appointment.getUser().getId();
        slotLockService.releaseLock(businessId, staffId, appointment.getStartTime(), appointmentUserId);

        Appointment saved = appointmentRepository.save(appointment);

        try {
            notificationService.sendToUser(saved.getUser().getId(), PushNotificationPayload.builder()
                    .title("Randevunuz İptal Edildi")
                    .body(saved.getBusiness().getName() + " randevunuzu iptal etti | " + formatDateTime(saved.getStartTime()))
                    .type(NotificationType.APPOINTMENT_CANCELLED)
                    .data(Map.of("appointmentId", saved.getId().toString(), "screen", "appointment_detail"))
                    .build());
        } catch (Exception e) {
            log.warn("Kullanıcıya randevu iptal bildirimi gönderilemedi: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>İzin verilen kaynak durum: {@code CONFIRMED}
     */
    @Override
    @Transactional
    public AppointmentResponse completeAppointment(UUID appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        requireBusinessOwnershipForAppointment(appointment);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Sadece CONFIRMED durumundaki randevu tamamlanabilir.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointmentRepository.save(appointment));
    }

    /**
     * {@inheritDoc}
     *
     * <p>İzin verilen kaynak durum: {@code CONFIRMED}
     */
    @Override
    @Transactional
    public AppointmentResponse markNoShow(UUID appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        requireBusinessOwnershipForAppointment(appointment);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Sadece CONFIRMED durumundaki randevu no-show olarak işaretlenebilir.");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return toResponse(appointmentRepository.save(appointment));
    }

    // =========================================================================
    // Private yardımcı metodlar
    // =========================================================================

    private String formatDateTime(OffsetDateTime dt) {
        if (dt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy EEEE HH:mm", new Locale("tr", "TR"));
        return dt.format(formatter);
    }

    /**
     * Randevuyu ID ile getirir; bulunamazsa {@link ResourceNotFoundException} fırlatır.
     *
     * @param appointmentId sorgulanacak randevunun UUID'si
     * @return bulunan randevu entity'si
     */
    private Appointment findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Randevu bulunamadı: " + appointmentId));
    }

    /**
     * Belirli bir işletmenin sahibinin giriş yapmış kullanıcı olup olmadığını kontrol eder.
     * Değilse {@link ForbiddenException} fırlatır.
     *
     * @param businessId doğrulanacak işletmenin UUID'si
     */
    private void requireBusinessOwnership(UUID businessId) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        businessRepository.findByIdAndOwnerIdAndIsActiveTrue(businessId, currentUserId)
                .orElseThrow(() -> new ForbiddenException(
                        "Bu işletmenin randevularını görüntüleme yetkiniz yok."));
    }

    /**
     * Randevunun ait olduğu işletmenin sahibinin giriş yapmış kullanıcı olup olmadığını kontrol eder.
     * Değilse {@link ForbiddenException} fırlatır.
     *
     * @param appointment yetki kontrolü yapılacak randevu
     */
    private void requireBusinessOwnershipForAppointment(Appointment appointment) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        UUID ownerId = appointment.getBusiness().getOwner().getId();
        if (!ownerId.equals(currentUserId)) {
            throw new ForbiddenException("Bu işlem için işletme sahibi yetkisi gereklidir.");
        }
    }

    /**
     * {@link Appointment} entity'sini {@link AppointmentResponse} record'una dönüştürür.
     *
     * <p>Staff alanı null-safe ele alınır; personelsiz işletmelerde {@code staffId}
     * ve {@code staffName} null olarak set edilir.
     *
     * @param appointment dönüştürülecek randevu entity'si
     * @return doldurulmuş yanıt record'u
     */
    private AppointmentResponse toResponse(Appointment appointment) {
        Staff staff = appointment.getStaff();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getUser().getId(),
                appointment.getUser().getFullName(),
                appointment.getBusiness().getId(),
                appointment.getBusiness().getName(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getService().getDurationMin(),
                staff != null ? staff.getId()       : null,
                staff != null ? staff.getFullName() : null,
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getPriceSnapshot(),
                appointment.getCurrency(),
                appointment.getNotes(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt()
        );
    }
}
