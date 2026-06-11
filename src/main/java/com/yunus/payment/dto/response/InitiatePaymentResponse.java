package com.yunus.payment.dto.response;

import java.util.UUID;

/**
 * Ödeme başlatıldığında dönen yanıt; htmlContent iframe'de gösterilir.
 */
public record InitiatePaymentResponse(

        // Bizim veritabanımızdaki ödeme kaydının UUID'si
        UUID paymentId,

        // iyzico'dan dönen, 3DS doğrulama formunu içeren HTML içeriği
        String htmlContent

) {}
