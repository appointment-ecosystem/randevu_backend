package com.yunus.business.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Yeni hizmet oluşturma isteği.
 * Validation anotasyonları controller'da @Valid ile tetiklenir.
 */
public record CreateServiceRequest(

        @NotBlank(message = "Hizmet adı boş olamaz")
        String name,

        // Açıklama isteğe bağlı; null gelebilir
        String description,

        @NotNull(message = "Süre zorunludur")
        @Min(value = 1, message = "Süre en az 1 dakika olmalıdır")
        Integer durationMin,

        @NotNull(message = "Fiyat zorunludur")
        @DecimalMin(value = "0.01", message = "Fiyat 0.01'den küçük olamaz")
        BigDecimal price,

        // Para birimi belirtilmezse TRY varsayılan olarak kullanılır
        String currency,

        // Sıralama belirtilmezse 0 kabul edilir
        Integer sortOrder
) {
    /**
     * Compact constructor: null gelirse varsayılan değerleri uygular.
     * Record'larda bu pattern field başlangıç değeri yerine kullanılır.
     */
    public CreateServiceRequest {
        if (currency == null || currency.isBlank()) {
            currency = "TRY";
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
