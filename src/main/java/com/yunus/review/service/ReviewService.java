package com.yunus.review.service;

/**
 * Değerlendirme (Review) işlemlerini tanımlayan servis arayüzü.
 * <p>
 * Yorum oluşturma, işletmeye veya giriş yapan kullanıcıya göre listeleme
 * ve soft-delete ile yorum gizleme operasyonlarını kapsar.
 * Implementasyon: ReviewServiceImpl
 * </p>
 */

import com.yunus.review.dto.CreateReviewRequest;
import com.yunus.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    /**
     * Tamamlanmış bir randevuya ait yeni değerlendirme oluşturur.
     *
     * @param request Yorum bilgilerini taşıyan istek DTO'su
     * @return Oluşturulan değerlendirmenin yanıt DTO'su
     */
    ReviewResponse createReview(CreateReviewRequest request);

    /**
     * Belirtilen işletmeye ait ve görünür (isVisible = true) değerlendirmeleri sayfalı döner.
     *
     * @param businessId Değerlendirmelerin sorgulanacağı işletme UUID'si
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return Görünür değerlendirmelerin sayfalı listesi
     */
    Page<ReviewResponse> getBusinessReviews(UUID businessId, Pageable pageable);

    /**
     * Giriş yapan kullanıcının tüm değerlendirmelerini sayfalı döner.
     *
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Kullanıcıya ait değerlendirmelerin sayfalı listesi
     */
    Page<ReviewResponse> getMyReviews(Pageable pageable);

    /**
     * Giriş yapan kullanıcının kendi yorumunu soft-delete ile gizler (isVisible = false).
     *
     * @param reviewId Gizlenecek değerlendirmenin UUID'si
     */
    void deleteReview(UUID reviewId);
}
