package com.yunus.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * iyzico 3DS ödeme başlatma isteği.
 */
public record InitiatePaymentRequest(

        // Ödemesi yapılacak randevunun UUID'si
        @NotNull(message = "Randevu ID'si zorunludur")
        UUID appointmentId,

        // Kart üzerinde yazan ad soyad
        @NotBlank(message = "Kart üzerindeki isim zorunludur")
        String cardHolderName,

        // Boşluksuz, sadece rakamlardan oluşmalıdır; servis katmanında temizlenir
        @NotBlank(message = "Kart numarası zorunludur")
        String cardNumber,

        // "01" ile "12" arasında, iki haneli ay
        @NotBlank(message = "Son kullanma ayı zorunludur")
        String expireMonth,

        // Dört haneli yıl, örn. "2026"
        @NotBlank(message = "Son kullanma yılı zorunludur")
        String expireYear,

        // Kartın arkasındaki güvenlik kodu
        @NotBlank(message = "CVC zorunludur")
        String cvc,

        // TC kimlik numarası; verilmezse sandbox testleri için "11111111111" kullanılır
        String buyerIdentityNumber

) {}
