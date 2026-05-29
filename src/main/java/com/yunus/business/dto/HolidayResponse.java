package com.yunus.business.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tatil bilgisini API'ye taşıyan yanıt kaydı.
 * staffId null ise işletme geneli, dolu ise personele özel tatildir.
 */
public record HolidayResponse(
        UUID id,
        UUID businessId,
        // null = tüm işletme kapalı; dolu = sadece bu personel
        UUID staffId,
        LocalDate date,
        String reason
) {
}
