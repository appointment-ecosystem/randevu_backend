package com.yunus.business.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tatil / kapalı gün ekleme isteği.
 * staffId null ise tüm işletme kapalı; dolu ise yalnızca o personel.
 */
public record HolidayRequest(

        @NotNull(message = "Tarih zorunludur")
        LocalDate date,

        // Tatil gerekçesi isteğe bağlı (bayram, izin, tadilat vb.)
        String reason,

        // null = işletme geneli tatil; dolu = personele özel tatil/izin
        UUID staffId
) {
}
