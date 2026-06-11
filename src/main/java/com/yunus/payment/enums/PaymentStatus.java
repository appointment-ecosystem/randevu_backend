package com.yunus.payment.enums;

/**
 * Ödeme işleminin gateway ve iş akışı içindeki durumunu temsil eder.
 */
public enum PaymentStatus {

    // Ödeme süreci başlatıldı, henüz sonuçlanmadı
    INITIATED,

    // Ödeme onay bekliyor (örn. 3D Secure doğrulaması)
    PENDING,

    // Ödeme başarıyla tamamlandı
    SUCCESS,

    // Ödeme başarısız oldu
    FAIL,

    // Ödeme iptal edildi
    CANCELLED,

    // Ödeme tamamen iade edildi
    REFUNDED,

    // Ödeme kısmen iade edildi
    PARTIALLY_REFUNDED

}
