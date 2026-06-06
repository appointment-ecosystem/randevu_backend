package com.yunus.appointment.service;

import com.yunus.appointment.dto.AppointmentResponse;
import com.yunus.appointment.dto.CreateAppointmentRequest;
import com.yunus.appointment.entity.AppointmentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Randevu yaşam döngüsü işlemlerinin servis sözleşmesi.
 *
 * <p>Sorumluluklar:
 * <ul>
 *   <li>Randevu oluşturma — slot lock + çakışma kontrolü ile</li>
 *   <li>Tekil ve toplu randevu sorgulama</li>
 *   <li>Durum geçişleri: onay, iptal (kullanıcı/işletme), tamamlama, no-show</li>
 * </ul>
 */
public interface AppointmentService {

    /**
     * Yeni bir randevu oluşturur.
     *
     * <p>Redis slot lock alınır → DB çakışma kontrolü yapılır → kaydedilir.
     * Lock kasıtlı olarak bırakılmaz; 5 dakikalık TTL ile kendiliğinden sona erer.
     *
     * @param request işletme, hizmet, personel (opsiyonel), başlangıç zamanı ve not
     * @return oluşturulan randevunun detay yanıtı
     */
    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    /**
     * Tek bir randevuyu getirir.
     *
     * <p>Yetki: randevunun sahibi (user) veya işletme sahibi (business.owner)
     * görüntüleyebilir.
     *
     * @param appointmentId sorgulanacak randevunun UUID'si
     * @return randevu detay yanıtı
     */
    AppointmentResponse getAppointment(UUID appointmentId);

    /**
     * Giriş yapmış kullanıcının tüm randevularını döner (en yeni → en eski).
     *
     * @return kullanıcının randevu listesi
     */
    List<AppointmentResponse> getMyAppointments();

    /**
     * Giriş yapmış kullanıcının belirli statüdeki randevularını döner.
     *
     * @param status filtrelenmek istenen randevu durumu
     * @return kullanıcının ilgili statüdeki randevuları
     */
    List<AppointmentResponse> getMyAppointments(AppointmentStatus status);

    /**
     * Belirli bir kullanıcının tüm randevularını döner (en yeni → en eski).
     *
     * @param userId randevuları getirilecek kullanıcının UUID'si
     * @return kullanıcının randevu listesi
     */
    List<AppointmentResponse> getUserAppointments(UUID userId);

    /**
     * İşletmenin verilen tarih aralığındaki tüm randevularını döner.
     *
     * <p>Yetki: yalnızca işletme sahibi erişebilir.
     *
     * @param businessId işletmenin UUID'si
     * @param rangeStart aralık başlangıcı (dahil)
     * @param rangeEnd   aralık sonu (hariç)
     * @return işletmenin randevuları, {@code startTime ASC} sıralı
     */
    List<AppointmentResponse> getBusinessAppointments(
            UUID businessId, OffsetDateTime rangeStart, OffsetDateTime rangeEnd);

    /**
     * İşletmenin verilen tarih aralığındaki belirli statüdeki randevularını döner.
     *
     * <p>Yetki: yalnızca işletme sahibi erişebilir.
     *
     * @param businessId işletmenin UUID'si
     * @param status     filtre durumu
     * @param rangeStart aralık başlangıcı (dahil)
     * @param rangeEnd   aralık sonu (hariç)
     * @return işletmenin ilgili statüdeki randevuları, {@code startTime ASC} sıralı
     */
    List<AppointmentResponse> getBusinessAppointments(
            UUID businessId, AppointmentStatus status,
            OffsetDateTime rangeStart, OffsetDateTime rangeEnd);

    /**
     * PENDING durumundaki randevuyu CONFIRMED'a geçirir.
     *
     * <p>Yetki: yalnızca işletme sahibi onaylayabilir.
     *
     * @param appointmentId onaylanacak randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    AppointmentResponse confirmAppointment(UUID appointmentId);

    /**
     * Kullanıcı tarafından randevuyu CANCELLED_BY_USER'a geçirir.
     *
     * <p>Yetki: yalnızca randevunun sahibi iptal edebilir.
     *
     * @param appointmentId iptal edilecek randevunun UUID'si
     * @param reason        iptal gerekçesi (opsiyonel)
     * @return güncellenmiş randevu yanıtı
     */
    AppointmentResponse cancelAppointmentByUser(UUID appointmentId, String reason);

    /**
     * İşletme tarafından randevuyu CANCELLED_BY_BUSINESS'a geçirir.
     *
     * <p>Yetki: yalnızca işletme sahibi iptal edebilir.
     *
     * @param appointmentId iptal edilecek randevunun UUID'si
     * @param reason        iptal gerekçesi (opsiyonel)
     * @return güncellenmiş randevu yanıtı
     */
    AppointmentResponse cancelAppointmentByBusiness(UUID appointmentId, String reason);

    /**
     * CONFIRMED durumundaki randevuyu COMPLETED'a geçirir.
     *
     * <p>Yetki: yalnızca işletme sahibi tamamlayabilir.
     *
     * @param appointmentId tamamlanacak randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    AppointmentResponse completeAppointment(UUID appointmentId);

    /**
     * CONFIRMED durumundaki randevuyu NO_SHOW'a geçirir.
     * Kullanıcının randevuya gelmediği durumlarda kullanılır.
     *
     * <p>Yetki: yalnızca işletme sahibi işaretleyebilir.
     *
     * @param appointmentId no-show işaretlenecek randevunun UUID'si
     * @return güncellenmiş randevu yanıtı
     */
    AppointmentResponse markNoShow(UUID appointmentId);
}
