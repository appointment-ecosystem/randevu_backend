package com.yunus.review.controller;

/**
 * Değerlendirme (Review) endpoint'lerini sunan REST controller.
 * <p>
 * Endpoint listesi:
 *   POST   /api/v1/reviews                        — Yeni yorum oluştur (kimlik doğrulama gerekli)
 *   GET    /api/v1/reviews/business/{businessId}  — İşletmenin görünür yorumlarını listele (herkese açık)
 *   GET    /api/v1/reviews/my                     — Giriş yapan kullanıcının yorumlarını listele
 *   DELETE /api/v1/reviews/{reviewId}             — Kendi yorumunu soft-delete ile gizle
 * </p>
 */

import com.yunus.common.response.BaseResponse;
import com.yunus.review.dto.CreateReviewRequest;
import com.yunus.review.dto.ReviewResponse;
import com.yunus.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Kullanıcı değerlendirme ve puan yönetimi")
@RequiredArgsConstructor
public class ReviewController {

    // Değerlendirme iş mantığını yürüten servis
    private final ReviewService reviewService;

    /**
     * Tamamlanmış bir randevuya ait yeni değerlendirme oluşturur.
     * Giriş yapmış kullanıcı zorunludur.
     *
     * @param request Değerlendirme bilgilerini taşıyan istek gövdesi
     * @return Oluşturulan değerlendirme bilgisi, HTTP 201 Created
     */
    @Operation(summary = "Değerlendirme oluştur", description = "Tamamlanmış randevuya yorum ve puan ekler")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success("Değerlendirme başarıyla oluşturuldu", response));
    }

    /**
     * Belirtilen işletmeye ait görünür değerlendirmeleri sayfalı döner.
     * Herkese açık endpoint; kimlik doğrulama gerekmez.
     *
     * @param businessId Değerlendirmeleri listelenecek işletmenin UUID'si
     * @param page       Sayfa numarası (varsayılan: 0)
     * @param size       Sayfa başına kayıt sayısı (varsayılan: 20)
     * @return Görünür değerlendirmelerin sayfalı listesi
     */
    @Operation(summary = "İşletme değerlendirmelerini listele", description = "İşletmeye ait görünür yorumları sayfalı döner")
    @GetMapping("/business/{businessId}")
    public ResponseEntity<BaseResponse<Page<ReviewResponse>>> getBusinessReviews(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getBusinessReviews(businessId, pageable);
        return ResponseEntity.ok(BaseResponse.success(reviews));
    }

    /**
     * Giriş yapan kullanıcının tüm değerlendirmelerini sayfalı döner.
     * Giriş yapmış kullanıcı zorunludur.
     *
     * @param page Sayfa numarası (varsayılan: 0)
     * @param size Sayfa başına kayıt sayısı (varsayılan: 20)
     * @return Kullanıcıya ait değerlendirmelerin sayfalı listesi
     */
    @Operation(summary = "Kendi değerlendirmelerimi listele", description = "Giriş yapan kullanıcının tüm yorumlarını döner")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Page<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewResponse> reviews = reviewService.getMyReviews(pageable);
        return ResponseEntity.ok(BaseResponse.success(reviews));
    }

    /**
     * Giriş yapan kullanıcının belirtilen değerlendirmesini soft-delete ile gizler.
     * Kullanıcı yalnızca kendi yorumunu silebilir.
     *
     * @param reviewId Gizlenecek değerlendirmenin UUID'si
     * @return Başarı mesajı, HTTP 200 OK
     */
    @Operation(summary = "Değerlendirme sil", description = "Kendi yorumunu isVisible=false yaparak gizler")
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(BaseResponse.success("Değerlendirme başarıyla silindi"));
    }
}
