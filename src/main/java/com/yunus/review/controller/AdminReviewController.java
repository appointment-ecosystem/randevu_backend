package com.yunus.review.controller;

import com.yunus.common.response.BaseResponse;
import com.yunus.review.dto.AdminReviewResponse;
import com.yunus.review.service.ReviewService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin rolüne sahip kullanıcıların yorum moderasyonu işlemlerini
 * (filtreleyerek listeleme, gizleme, görünür yapma) gerçekleştirmesini
 * sağlayan denetleyici sınıf.
 * Tüm endpoint'ler ROLE_ADMIN yetkisi gerektirir.
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param reviewService Yorum yönetim servisi
     */
    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Tüm yorumları opsiyonel filtrelerle sayfalanmış olarak listeler.
     * GET /api/v1/admin/reviews
     *
     * <p>Filtre kombinasyonları:
     * <ul>
     *   <li>Filtre yok → tüm yorumlar</li>
     *   <li>businessId → yalnızca o işletmenin yorumları</li>
     *   <li>isVisible → yalnızca görünür/gizli yorumlar</li>
     *   <li>Her ikisi → her iki koşula uyan yorumlar</li>
     * </ul>
     *
     * @param businessId Filtrelenecek işletme UUID'si (opsiyonel)
     * @param isVisible  Görünürlük filtresi (opsiyonel)
     * @param page       Sayfa numarası (varsayılan 0)
     * @param size       Sayfadaki kayıt sayısı (varsayılan 20)
     * @return Filtrelenmiş ve sayfalanmış yorum listesi yanıtı
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AdminReviewResponse>>> getAllReviews(
            @RequestParam(required = false) UUID businessId,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminReviewResponse> response = reviewService.getAllReviews(businessId, isVisible, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen yorumu gizler (isVisible = false).
     * Yorum bulunamazsa HTTP 404 döner.
     * PATCH /api/v1/admin/reviews/{reviewId}/hide
     *
     * @param reviewId Gizlenecek yorumun UUID'si
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<BaseResponse<Void>> hideReview(@PathVariable UUID reviewId) {
        reviewService.hideReview(reviewId);
        return ResponseEntity.ok(BaseResponse.success("Yorum başarıyla gizlendi"));
    }

    /**
     * Gizlenmiş bir yorumu tekrar görünür yapar (isVisible = true).
     * Yorum bulunamazsa HTTP 404 döner.
     * PATCH /api/v1/admin/reviews/{reviewId}/show
     *
     * @param reviewId Görünür yapılacak yorumun UUID'si
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{reviewId}/show")
    public ResponseEntity<BaseResponse<Void>> showReview(@PathVariable UUID reviewId) {
        reviewService.showReview(reviewId);
        return ResponseEntity.ok(BaseResponse.success("Yorum başarıyla görünür yapıldı"));
    }
}
