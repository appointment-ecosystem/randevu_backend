package com.yunus.review.dto;

/**
 * Yeni bir değerlendirme (review) oluşturmak için kullanılan istek DTO'su.
 * <p>
 * Kullanıcı; tamamlanmış bir randevuya ait UUID, 1–5 arası puan ve
 * opsiyonel bir yorum metni göndererek yeni değerlendirme oluşturabilir.
 * Bean Validation anotasyonları ile giriş verisi doğrulanır.
 * </p>
 */

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReviewRequest(

        // Değerlendirmenin bağlı olduğu randevunun benzersiz kimliği — zorunlu
        @NotNull(message = "Randevu ID boş olamaz")
        UUID appointmentId,

        // Kullanıcının verdiği puan: 1 (en düşük) ile 5 (en yüksek) arasında — zorunlu
        @NotNull(message = "Puan boş olamaz")
        @Min(value = 1, message = "Puan en az 1 olmalıdır")
        @Max(value = 5, message = "Puan en fazla 5 olabilir")
        Integer rating,

        // Kullanıcının yazdığı yorum metni — opsiyonel, maksimum 1000 karakter
        @Size(max = 1000, message = "Yorum en fazla 1000 karakter olabilir")
        String comment

) {}
