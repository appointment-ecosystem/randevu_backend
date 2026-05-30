package com.yunus.appointment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Belirli bir işletme, hizmet ve tarih kombinasyonu için müsait slot listesini
 * sorgulamak amacıyla kullanılan istek nesnesi.
 *
 * <p>Kullanım yeri:
 * <ul>
 *   <li>{@code POST /api/v1/appointments/available-slots} — istemcinin slot
 *       listesi talep etmesini sağlar.</li>
 * </ul>
 *
 * <p>Notlar:
 * <ul>
 *   <li>{@code staffId} isteğe bağlıdır; gönderilmezse sistemin herhangi bir
 *       uygun personeli için slotlar hesaplanır.</li>
 *   <li>{@code date} alanı {@link LocalDate} kullanır; bu alan zaman dilimi
 *       bağımsız (takvim günü) bir değerdir.</li>
 * </ul>
 *
 * @param businessId işletmenin benzersiz tanımlayıcısı (zorunlu)
 * @param serviceId  sorgulanacak hizmetin benzersiz tanımlayıcısı (zorunlu)
 * @param staffId    tercih edilen personelin UUID'si; {@code null} ise herhangi bir uygun personel
 * @param date       müsaitlik sorgulanacak takvim günü (zorunlu)
 */
public record AvailableSlotsRequest(
        @NotNull UUID businessId,
        @NotNull UUID serviceId,
        UUID staffId,
        @NotNull LocalDate date
) {}
