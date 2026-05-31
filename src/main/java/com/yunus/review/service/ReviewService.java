package com.yunus.review.service;

/**
 * Değerlendirme (Review) işlemlerini tanımlayan servis arayüzü.
 * <p>
 * Yorum oluşturma, işletmeye veya giriş yapan kullanıcıya göre listeleme
 * ve soft-delete ile yorum gizleme operasyonlarını kapsar.
 * Admin paneli için: tüm yorumları filtreleyerek listeleme, yorumu gizleme/gösterme.
 * Implementasyon: ReviewServiceImpl
 * </p>
 */

import com.yunus.review.dto.AdminReviewResponse;
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

    // ── Admin yorum yönetimi metodları ──────────────────────────────────────────

    /**
     * Admin paneli için tüm yorumları opsiyonel filtrelerle sayfalı döner.
     * <p>
     * Filtre mantığı:
     * <ul>
     *   <li>businessId null, isVisible null → tüm yorumlar</li>
     *   <li>businessId var, isVisible null → işletmeye göre filtreli</li>
     *   <li>businessId null, isVisible var → görünürlüğe göre filtreli</li>
     *   <li>Her ikisi de var → her iki filtreye göre birlikte</li>
     * </ul>
     *
     * @param businessId Filtrelenecek işletme UUID'si (opsiyonel, null ise tüm işletmeler)
     * @param isVisible  Görünürlük filtresi (opsiyonel, null ise her iki durum)
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return Filtrelenmiş yorumların sayfalı listesi
     */
    Page<AdminReviewResponse> getAllReviews(UUID businessId, Boolean isVisible, Pageable pageable);

    /**
     * Belirtilen yorumu admin tarafından gizler (isVisible = false).
     * Yorum bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param reviewId Gizlenecek yorumun UUID'si
     */
    void hideReview(UUID reviewId);

    /**
     * Gizlenmiş bir yorumu admin tarafından tekrar görünür yapar (isVisible = true).
     * Yorum bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param reviewId Görünür yapılacak yorumun UUID'si
     */
    void showReview(UUID reviewId);
}

