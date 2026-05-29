package com.yunus.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Tek bir günün çalışma saati verisi.
 * isClosed=false ise openTime ve closeTime zorunludur;
 * bu kural service katmanında validate edilir (entity'de nullable=false olduğu için).
 */
public record WorkingHourRequest(

        @NotNull(message = "Gün numarası zorunludur")
        @Min(value = 1, message = "Gün numarası en az 1 (Pazartesi) olmalıdır")
        @Max(value = 7, message = "Gün numarası en fazla 7 (Pazar) olmalıdır")
        Integer dayOfWeek,

        // isClosed=true ise anlamsız ama kabul edilir; service katmanı yönetir
        LocalTime openTime,

        LocalTime closeTime,

        @NotNull(message = "Kapalı durumu zorunludur")
        Boolean isClosed
) {
}
