package com.yunus.appointment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Müsait ya da dolu bir zaman dilimini (slot) temsil eden yanıt nesnesi.
 *
 * <p>Kullanım yeri:
 * <ul>
 *   <li>{@code GET /api/v1/appointments/available-slots} — kullanıcıya sunulacak
 *       rezervasyon pencerelerini listeler.</li>
 * </ul>
 *
 * <p>Notlar:
 * <ul>
 *   <li>{@code staffId} ve {@code staffName} personel seçimi zorunlu olmayan
 *       işletmelerde {@code null} olabilir.</li>
 *   <li>{@code available = false} olan slotlar randevu alınmış ya da kilitli
 *       (Redis lock) anlamına gelir; UI'da devre dışı gösterilmelidir.</li>
 * </ul>
 *
 * @param staffId    ilgili personelin UUID'si; personelsiz işletmelerde {@code null}
 * @param staffName  ilgili personelin görünen adı; personelsiz işletmelerde {@code null}
 * @param startTime  slotun başlangıç zamanı (saat dilimi bilgisiyle birlikte)
 * @param endTime    slotun bitiş zamanı (saat dilimi bilgisiyle birlikte)
 * @param available  {@code true} ise slot rezerve edilebilir, {@code false} ise dolu/kilitli
 */
public record SlotResponse(
        UUID staffId,
        String staffName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        boolean available
) {}
