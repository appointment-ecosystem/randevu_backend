package com.yunus.payment.dto.response;

import com.yunus.payment.enums.PaymentStatus;
import com.yunus.payment.enums.PaymentType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * Ödeme kaydının genel durum yanıtı.
 */
@Builder
public record PaymentResponse(

        // Ödeme kaydının UUID'si
        UUID id,

        // Ödemenin ait olduğu randevunun UUID'si
        UUID appointmentId,

        // Ödeme tutarı
        BigDecimal amount,

        // Para birimi (örn. "TRY")
        String currency,

        // DEPOSIT (kapora) veya FULL (tam ödeme)
        PaymentType paymentType,

        // Ödemenin güncel durumu
        PaymentStatus status,

        // Ödeme sağlayıcısı (örn. "IYZICO")
        String provider,

        // iyzico'nun döndürdüğü paymentId; ödeme henüz tamamlanmadıysa null olabilir
        String providerReference,

        // Ödeme kaydının oluşturulma zamanı
        OffsetDateTime createdAt,

        // Ödeme kaydının son güncellenme zamanı
        OffsetDateTime updatedAt

) {}
