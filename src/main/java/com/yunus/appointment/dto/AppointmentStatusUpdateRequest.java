package com.yunus.appointment.dto;

import com.yunus.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Mevcut bir randevunun durumunu güncellemek için kullanılan istek nesnesi.
 *
 * <p>Kullanım yeri:
 * <ul>
 *   <li>{@code PATCH /api/v1/appointments/{id}/status} — işletme sahibi veya
 *       yetkili kullanıcı tarafından randevu durumu değiştirilirken kullanılır.</li>
 * </ul>
 *
 * <p>İzin verilen durum geçişleri (örnek):
 * <ul>
 *   <li>{@code PENDING} → {@code CONFIRMED} (işletme onayı)</li>
 *   <li>{@code PENDING / CONFIRMED} → {@code CANCELLED_BY_USER} (kullanıcı iptali)</li>
 *   <li>{@code PENDING / CONFIRMED} → {@code CANCELLED_BY_BUSINESS} (işletme iptali)</li>
 *   <li>{@code CONFIRMED} → {@code COMPLETED} (hizmet tamamlandı)</li>
 *   <li>{@code CONFIRMED} → {@code NO_SHOW} (kullanıcı gelmedi)</li>
 * </ul>
 *
 * <p>Notlar:
 * <ul>
 *   <li>{@code reason} alanı yalnızca {@code CANCELLED_BY_USER} veya
 *       {@code CANCELLED_BY_BUSINESS} durumlarına geçişte anlamlıdır;
 *       diğer geçişlerde {@code null} gönderilebilir.</li>
 *   <li>Geçersiz durum geçiş kombinasyonları servis katmanında
 *       {@code IllegalStateException} fırlatır.</li>
 * </ul>
 *
 * @param newStatus geçilmek istenen yeni randevu durumu (zorunlu)
 * @param reason    iptal gerekçesi; yalnızca iptal durumlarına geçişte kullanılır
 */
public record AppointmentStatusUpdateRequest(
        @NotNull AppointmentStatus newStatus,
        String reason
) {}
