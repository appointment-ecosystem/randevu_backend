package com.yunus.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Mevcut hizmet güncelleme isteği.
 * imageUrl bu DTO'da yoktur; görsel yönetimi ayrı endpoint'ten yapılır.
 */
public record UpdateServiceRequest(

        @NotBlank(message = "Hizmet adı boş olamaz")
        String name,

        // Açıklama isteğe bağlı; null göndererek temizlenebilir
        String description,

        @NotNull(message = "Süre zorunludur")
        @Min(value = 1, message = "Süre en az 1 dakika olmalıdır")
        Integer durationMin,

        @NotNull(message = "Fiyat zorunludur")
        @DecimalMin(value = "0.01", message = "Fiyat 0.01'den küçük olamaz")
        BigDecimal price,

        // Para birimi null gelirse mevcut değer korunur
        String currency,

        // Sıralama null gelirse mevcut değer korunur
        Integer sortOrder
) {
}
