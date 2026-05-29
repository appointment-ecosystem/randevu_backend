package com.yunus.business.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Çalışma saati bilgisini API'ye taşıyan yanıt kaydı.
 * staffId null ise işletme geneli, dolu ise personele özel saatlerdir.
 */
public record WorkingHourResponse(
        UUID id,
        UUID businessId,
        // null = işletme geneli; dolu = personele özel
        UUID staffId,
        Integer dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean isClosed
) {
}
