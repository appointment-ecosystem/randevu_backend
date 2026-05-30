package com.yunus.appointment.dto;

import com.yunus.appointment.entity.AppointmentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bir randevunun tüm detaylarını içeren yanıt nesnesi.
 *
 * <p>Kullanım yerleri:
 * <ul>
 *   <li>{@code POST /api/v1/appointments} — başarılı oluşturmanın ardından
 *       oluşturulan randevu kaydını döner.</li>
 *   <li>{@code GET /api/v1/appointments/{id}} — tek bir randevunun detaylarını
 *       getirir.</li>
 *   <li>{@code GET /api/v1/appointments/my} — kullanıcının kendi randevu
 *       listesini döner.</li>
 *   <li>{@code GET /api/v1/business/{businessId}/appointments} — işletme
 *       sahibinin randevu listesini döner.</li>
 *   <li>{@code PATCH /api/v1/appointments/{id}/status} — durum güncellemesinin
 *       ardından güncel randevu kaydını döner.</li>
 * </ul>
 *
 * <p>Notlar:
 * <ul>
 *   <li>{@code staffId} ve {@code staffName} personel seçimi zorunlu olmayan
 *       işletmelerde {@code null} olabilir.</li>
 *   <li>{@code cancellationReason} yalnızca {@code CANCELLED_BY_USER} veya
 *       {@code CANCELLED_BY_BUSINESS} durumlarında dolu olabilir.</li>
 *   <li>{@code priceSnapshot}, randevu oluşturulduğu andaki hizmet fiyatıdır;
 *       sonradan {@code Service.price} değişse bile bu değer sabit kalır.</li>
 * </ul>
 *
 * @param id                 randevunun benzersiz tanımlayıcısı
 * @param userId             randevu sahibi kullanıcının UUID'si
 * @param userName           kullanıcının görünen adı
 * @param businessId         randevunun yapıldığı işletmenin UUID'si
 * @param businessName       işletmenin görünen adı
 * @param serviceId          rezerve edilen hizmetin UUID'si
 * @param serviceName        hizmetin görünen adı
 * @param serviceDurationMin hizmetin dakika cinsinden süresi
 * @param staffId            atanan personelin UUID'si; personelsiz işletmelerde {@code null}
 * @param staffName          atanan personelin görünen adı; personelsiz işletmelerde {@code null}
 * @param startTime          randevunun başlangıç zamanı (saat dilimi bilgisiyle birlikte)
 * @param endTime            randevunun bitiş zamanı (saat dilimi bilgisiyle birlikte)
 * @param status             randevunun güncel durumu
 * @param priceSnapshot      randevu oluşturulduğu andaki hizmet fiyatı
 * @param currency           para birimi kodu (örn. "TRY", "USD")
 * @param notes              kullanıcının notu; {@code null} olabilir
 * @param cancellationReason iptal gerekçesi; yalnızca iptal durumlarında dolu
 * @param createdAt          randevunun oluşturulma zamanı
 */
public record AppointmentResponse(
        UUID id,
        UUID userId,
        String userName,
        UUID businessId,
        String businessName,
        UUID serviceId,
        String serviceName,
        Integer serviceDurationMin,
        UUID staffId,
        String staffName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        AppointmentStatus status,
        BigDecimal priceSnapshot,
        String currency,
        String notes,
        String cancellationReason,
        OffsetDateTime createdAt
) {}
