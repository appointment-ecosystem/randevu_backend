package com.yunus.review.dto;

/**
 * Değerlendirme (review) bilgilerini istemciye döndürmek için kullanılan yanıt DTO'su.
 * <p>
 * Review entity'sinden statik fabrika metodu (fromEntity) aracılığıyla oluşturulur.
 * Hassas veri gizlemek amacıyla yalnızca gerekli alanlar istemciye iletilir.
 * </p>
 */

import com.yunus.review.entity.Review;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(

        // Değerlendirmenin benzersiz kimliği
        UUID id,

        // Değerlendirmenin ait olduğu randevunun kimliği
        UUID appointmentId,

        // Değerlendirmenin yapıldığı işletmenin kimliği
        UUID businessId,

        // Değerlendirmeyi yapan kullanıcının kimliği
        UUID userId,

        // Değerlendirmeyi yapan kullanıcının tam adı
        String userFullName,

        // Kullanıcının verdiği puan (1–5)
        Integer rating,

        // Kullanıcının yazdığı yorum metni
        String comment,

        // Değerlendirmenin oluşturulma zamanı (BaseEntity'den)
        OffsetDateTime createdAt

) {

    /**
     * Review entity'sini ReviewResponse DTO'suna dönüştüren statik fabrika metodu.
     *
     * @param review Dönüştürülecek Review entity nesnesi
     * @return İstemciye gönderilecek ReviewResponse nesnesi
     */
    public static ReviewResponse fromEntity(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getAppointment().getId(),
                review.getBusiness().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
