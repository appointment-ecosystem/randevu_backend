package com.yunus.appointment.controller;

import com.yunus.appointment.dto.AppointmentResponse;
import com.yunus.appointment.dto.AppointmentStatusUpdateRequest;
import com.yunus.appointment.dto.CreateAppointmentRequest;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.service.AppointmentService;
import com.yunus.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.yunus.ratelimit.annotation.KeyType;
import com.yunus.ratelimit.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Randevu yaşam döngüsü endpoint'leri.
 *
 * <p>Tüm endpoint'ler kimlik doğrulaması gerektirir.
 * SecurityConfig'deki {@code .anyRequest().authenticated()} kuralı kapsamındadır.
 * Servis katmanı kendi içinde ek yetki kontrolü yapar (işletme sahibi vs. kullanıcı).
 *
 * <p>Endpoint listesi:
 * <ul>
 *   <li>POST   /api/v1/appointments              — randevu oluştur</li>
 *   <li>GET    /api/v1/appointments/{id}          — randevu detayı</li>
 *   <li>GET    /api/v1/appointments/my            — kendi randevularım</li>
 *   <li>GET    /api/v1/appointments/business/{id} — işletme takvimi</li>
 *   <li>PATCH  /api/v1/appointments/{id}/confirm  — onayla</li>
 *   <li>PATCH  /api/v1/appointments/{id}/cancel   — kullanıcı iptali</li>
 *   <li>PATCH  /api/v1/appointments/{id}/cancel-by-business — işletme iptali</li>
 *   <li>PATCH  /api/v1/appointments/{id}/complete — tamamla</li>
 *   <li>PATCH  /api/v1/appointments/{id}/no-show  — no-show işaretle</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Randevu yönetimi")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    // =========================================================================
    // Oluşturma
    // =========================================================================

    /**
     * Yeni bir randevu oluşturur.
     *
     * <p>Redis slot lock alınır, DB çakışma kontrolü yapılır ve randevu
     * PENDING statüsünde kaydedilir. Lock 5 dk TTL ile expire olur.
     *
     * <p>{@code POST /api/v1/appointments} → HTTP 201
     *
     * @param request işletme, hizmet, personel (opsiyonel), başlangıç zamanı ve not
     * @return oluşturulan randevunun detayı
     */
    // 1 saatlik pencerede aynı kullanıcıdan en fazla 20 randevu oluşturma isteği
    @RateLimit(limit = 20, windowSeconds = 3600, key = "appointment:create", keyType = KeyType.USER)
    @PostMapping
    @Operation(summary = "Randevu oluştur",
               description = "Slot lock ve DB çakışma kontrolü yapılarak PENDING statüsünde " +
                             "yeni randevu oluşturur.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {

        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    // =========================================================================
    // Sorgulama
    // =========================================================================

    /**
     * Giriş yapmış kullanıcının randevularını listeler.
     *
     * <p>Opsiyonel {@code status} parametresi ile filtreleme yapılabilir.
     *
     * <p>{@code GET /api/v1/appointments/my} → HTTP 200
     *
     * @param status filtre durumu; verilmezse tüm randevular döner
     * @return kullanıcının randevu listesi, startTime DESC sıralı
     */
    @GetMapping("/my")
    @Operation(summary = "Kendi randevularım",
               description = "Giriş yapmış kullanıcının randevularını döner. " +
                             "status parametresiyle filtrelenebilir.")
    public ResponseEntity<BaseResponse<List<AppointmentResponse>>> getMyAppointments(
            @RequestParam(required = false) AppointmentStatus status) {

        List<AppointmentResponse> appointments = (status != null)
                ? appointmentService.getMyAppointments(status)
                : appointmentService.getMyAppointments();

        return ResponseEntity.ok(BaseResponse.success(appointments));
    }

    /**
     * Belirtilen randevunun detayını getirir.
     *
     * <p>Yetki: randevunun sahibi veya ilgili işletmenin sahibi görüntüleyebilir.
     * Servis katmanında kontrol edilir.
     *
     * <p>{@code GET /api/v1/appointments/{id}} → HTTP 200
     *
     * @param id sorgulanacak randevunun UUID'si
     * @return randevu detay yanıtı
     */
    @GetMapping("/{id}")
    @Operation(summary = "Randevu detayı",
               description = "Randevunun sahibi veya işletme sahibi görüntüleyebilir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> getAppointment(
            @PathVariable UUID id) {

        AppointmentResponse response = appointmentService.getAppointment(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * İşletmenin belirtilen tarih aralığındaki randevularını listeler.
     *
     * <p>Opsiyonel {@code status} parametresi ile filtreleme yapılabilir.
     * Yetki: işletme sahibi; servis katmanında kontrol edilir.
     *
     * <p>{@code GET /api/v1/appointments/business/{businessId}} → HTTP 200
     *
     * @param businessId işletmenin UUID'si
     * @param rangeStart aralık başlangıcı (dahil) — ISO-8601 formatında
     * @param rangeEnd   aralık sonu (hariç) — ISO-8601 formatında
     * @param status     filtre durumu; verilmezse tüm statülerdeki randevular döner
     * @return işletmenin randevu listesi, startTime ASC sıralı
     */
    @GetMapping("/business/{businessId}")
    @Operation(summary = "İşletme randevuları (tarih aralığı)",
               description = "İşletme sahibinin belirtilen tarih aralığındaki randevularını döner. " +
                             "status ile filtreleme yapılabilir.")
    public ResponseEntity<BaseResponse<List<AppointmentResponse>>> getBusinessAppointments(
            @PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime rangeStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime rangeEnd,
            @RequestParam(required = false) AppointmentStatus status) {

        List<AppointmentResponse> appointments = (status != null)
                ? appointmentService.getBusinessAppointments(businessId, status, rangeStart, rangeEnd)
                : appointmentService.getBusinessAppointments(businessId, rangeStart, rangeEnd);

        return ResponseEntity.ok(BaseResponse.success(appointments));
    }

    // =========================================================================
    // Durum geçişleri
    // =========================================================================

    /**
     * PENDING durumundaki randevuyu CONFIRMED'a geçirir.
     *
     * <p>Yetki: yalnızca işletme sahibi onaylayabilir.
     *
     * <p>{@code PATCH /api/v1/appointments/{id}/confirm} → HTTP 200
     *
     * @param id onaylanacak randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Randevuyu onayla (işletme)",
               description = "PENDING durumundaki randevuyu CONFIRMED'a geçirir. " +
                             "Yalnızca işletme sahibi kullanabilir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> confirmAppointment(
            @PathVariable UUID id) {

        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(BaseResponse.success("Randevu onaylandı", response));
    }

    /**
     * Kullanıcı tarafından randevuyu iptal eder (CANCELLED_BY_USER).
     *
     * <p>Yetki: yalnızca randevunun sahibi iptal edebilir.
     *
     * <p>{@code PATCH /api/v1/appointments/{id}/cancel} → HTTP 200
     *
     * @param id      iptal edilecek randevunun UUID'si
     * @param request iptal gerekçesini içeren body (reason opsiyonel)
     * @return güncellenmiş randevu yanıtı
     */
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Randevuyu iptal et (kullanıcı)",
               description = "Kullanıcı kendi randevusunu CANCELLED_BY_USER statüsüne geçirir. " +
                             "reason opsiyoneldir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> cancelAppointmentByUser(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {

        AppointmentResponse response = appointmentService.cancelAppointmentByUser(id, request.reason());
        return ResponseEntity.ok(BaseResponse.success("Randevu iptal edildi", response));
    }

    /**
     * İşletme tarafından randevuyu iptal eder (CANCELLED_BY_BUSINESS).
     *
     * <p>Yetki: yalnızca işletme sahibi iptal edebilir.
     *
     * <p>{@code PATCH /api/v1/appointments/{id}/cancel-by-business} → HTTP 200
     *
     * @param id      iptal edilecek randevunun UUID'si
     * @param request iptal gerekçesini içeren body (reason opsiyonel)
     * @return güncellenmiş randevu yanıtı
     */
    @PatchMapping("/{id}/cancel-by-business")
    @Operation(summary = "Randevuyu iptal et (işletme)",
               description = "İşletme sahibi randevuyu CANCELLED_BY_BUSINESS statüsüne geçirir. " +
                             "reason opsiyoneldir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> cancelAppointmentByBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {

        AppointmentResponse response = appointmentService.cancelAppointmentByBusiness(id, request.reason());
        return ResponseEntity.ok(BaseResponse.success("Randevu işletme tarafından iptal edildi", response));
    }

    /**
     * CONFIRMED durumundaki randevuyu COMPLETED'a geçirir.
     *
     * <p>Yetki: yalnızca işletme sahibi tamamlayabilir.
     *
     * <p>{@code PATCH /api/v1/appointments/{id}/complete} → HTTP 200
     *
     * @param id tamamlanacak randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    @PatchMapping("/{id}/complete")
    @Operation(summary = "Randevuyu tamamlandı işaretle",
               description = "CONFIRMED durumundaki randevuyu COMPLETED'a geçirir. " +
                             "Yalnızca işletme sahibi kullanabilir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> completeAppointment(
            @PathVariable UUID id) {

        AppointmentResponse response = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(BaseResponse.success("Randevu tamamlandı olarak işaretlendi", response));
    }

    /**
     * CONFIRMED durumundaki randevuyu NO_SHOW'a geçirir.
     * Kullanıcının randevuya gelmediği durumlarda işletme tarafından kullanılır.
     *
     * <p>Yetki: yalnızca işletme sahibi işaretleyebilir.
     *
     * <p>{@code PATCH /api/v1/appointments/{id}/no-show} → HTTP 200
     *
     * @param id no-show işaretlenecek randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    @PatchMapping("/{id}/no-show")
    @Operation(summary = "No-show işaretle",
               description = "CONFIRMED durumundaki randevuyu NO_SHOW statüsüne geçirir. " +
                             "Yalnızca işletme sahibi kullanabilir.")
    public ResponseEntity<BaseResponse<AppointmentResponse>> markNoShow(
            @PathVariable UUID id) {

        AppointmentResponse response = appointmentService.markNoShow(id);
        return ResponseEntity.ok(BaseResponse.success("Randevu no-show olarak işaretlendi", response));
    }
}
