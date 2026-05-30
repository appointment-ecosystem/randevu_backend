package com.yunus.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Yeni bir randevu kaydı oluşturmak için istemciden alınan istek nesnesi.
 *
 * <p>Kullanım yeri:
 * <ul>
 *   <li>{@code POST /api/v1/appointments} — kimliği doğrulanmış kullanıcı
 *       tarafından randevu oluşturma işleminde kullanılır.</li>
 * </ul>
 *
 * <p>İş kuralları:
 * <ul>
 *   <li>{@code startTime}, müsaitlik kontrolünden geçmiş ve Redis ile
 *       kilitlenmiş bir slot başlangıcına karşılık gelmelidir.</li>
 *   <li>{@code staffId} personel seçimi zorunlu olmayan işletmelerde
 *       atlanabilir; bu durumda sistem uygun personeli otomatik atar veya
 *       {@code null} bırakır.</li>
 *   <li>Bitiş zamanı {@code startTime + service.durationMin} formülüyle
 *       servis katmanında hesaplanır, istemciden alınmaz.</li>
 * </ul>
 *
 * @param businessId işletmenin benzersiz tanımlayıcısı (zorunlu)
 * @param serviceId  rezerve edilecek hizmetin benzersiz tanımlayıcısı (zorunlu)
 * @param staffId    tercih edilen personelin UUID'si; {@code null} ise personel atanmaz
 * @param startTime  randevunun başlangıç zamanı (saat dilimi bilgisiyle birlikte, zorunlu)
 * @param notes      kullanıcının isteğe bağlı notu; maksimum 500 karakter
 */
public record CreateAppointmentRequest(
        @NotNull UUID businessId,
        @NotNull UUID serviceId,
        UUID staffId,
        @NotNull OffsetDateTime startTime,
        @Size(max = 500) String notes
) {}
