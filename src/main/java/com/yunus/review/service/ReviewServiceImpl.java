package com.yunus.review.service;

/**
 * ReviewService arayüzünün somut implementasyonu.
 * <p>
 * İş kuralları:
 *   - Yalnızca tamamlanmış (COMPLETED) randevular için yorum yapılabilir.
 *   - Aynı randevuya ikinci kez yorum yapılamaz.
 *   - Kullanıcı yalnızca kendi randevusuna yorum yapabilir.
 *   - Silme işlemi fiziksel değil; isVisible = false yapılarak soft-delete uygulanır.
 * </p>
 */

import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.exception.ErrorType;
import com.yunus.review.dto.AdminReviewResponse;
import com.yunus.review.dto.CreateReviewRequest;
import com.yunus.review.dto.ReviewResponse;
import com.yunus.review.entity.Review;
import com.yunus.review.repository.ReviewRepository;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    // Bağımlılıklar: repository ve yardımcı servisler
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    /**
     * Yeni değerlendirme oluşturur.
     * <p>
     * Adımlar:
     *   1. Giriş yapan kullanıcıyı al.
     *   2. Randevuyu bul; bulamazsa ResourceNotFoundException fırlat.
     *   3. Randevunun sahibi giriş yapan kullanıcı mı? Değilse ForbiddenException fırlat.
     *   4. Randevu durumu COMPLETED mi? Değilse BusinessException fırlat.
     *   5. Bu randevu için daha önce yorum yapılmış mı? Yapılmışsa BusinessException fırlat.
     *   6. Review entity'sini oluştur, kaydet ve ReviewResponse döndür.
     * </p>
     */
    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        // Adım 1: Giriş yapan kullanıcıyı al
        User currentUser = currentUserService.getCurrentUser();

        // Adım 2: Randevuyu bul
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Randevu bulunamadı: " + request.appointmentId()));

        // Adım 3: Randevunun sahibi mi kontrol et
        if (!appointment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu randevu için yorum yapma yetkiniz bulunmamaktadır");
        }

        // Adım 4: Randevu tamamlanmış mı kontrol et
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("Sadece tamamlanan randevular için yorum yapılabilir");
        }

        // Adım 5: Daha önce bu randevuya yorum yapılmış mı kontrol et
        if (reviewRepository.existsByAppointmentId(request.appointmentId())) {
            throw new BusinessException("Bu randevu için zaten yorum yapılmış");
        }

        // Adım 6: Review entity oluştur ve kaydet
        Review review = new Review();
        review.setAppointment(appointment);
        review.setUser(currentUser);
        review.setBusiness(appointment.getBusiness());
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setIsVisible(true);

        Review savedReview = reviewRepository.save(review);
        return ReviewResponse.fromEntity(savedReview);
    }

    /**
     * Belirtilen işletmeye ait görünür değerlendirmeleri sayfalı döner.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getBusinessReviews(UUID businessId, Pageable pageable) {
        return reviewRepository
                .findByBusinessIdAndIsVisibleTrue(businessId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    /**
     * Giriş yapan kullanıcının tüm değerlendirmelerini sayfalı döner.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Pageable pageable) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        return reviewRepository
                .findByUserId(currentUserId, pageable)
                .map(ReviewResponse::fromEntity);
    }

    /**
     * Giriş yapan kullanıcının belirtilen değerlendirmesini soft-delete ile gizler.
     * Hem review ID hem de user ID eşleşmelidir; eşleşme yoksa ResourceNotFoundException fırlat.
     */
    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        // Kullanıcının kendi yorumunu bul
        Review review = reviewRepository.findByIdAndUserId(reviewId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Değerlendirme bulunamadı veya bu değerlendirmeye erişim yetkiniz yok"));

        // Soft delete: fiziksel silme yerine gizle
        review.setIsVisible(false);
        reviewRepository.save(review);
    }

    // ── Admin yorum yönetimi metodları ──────────────────────────────────────────

    /**
     * Admin paneli için tüm yorumları opsiyonel businessId ve isVisible filtrelerine göre
     * sayfalı döner. Dört farklı kombinasyona göre doğru repository metodunu çağırır.
     *
     * @param businessId Filtrelenecek işletme UUID'si (null ise tüm işletmeler)
     * @param isVisible  Görünürlük filtresi (null ise her iki durum dahil edilir)
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return Filtrelenmiş yorumların AdminReviewResponse olarak sayfalı listesi
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAllReviews(UUID businessId, Boolean isVisible, Pageable pageable) {
        log.info("Admin yorum listeleme isteği — businessId: {}, isVisible: {}", businessId, isVisible);

        // 4 kombinasyon: businessId ve isVisible null/değer durumlarına göre doğru sorgu seç
        if (businessId != null && isVisible != null) {
            // Her iki filtre de var
            return reviewRepository
                    .findByBusinessIdAndIsVisibleOrderByCreatedAtDesc(businessId, isVisible, pageable)
                    .map(this::toAdminResponse);
        } else if (businessId != null) {
            // Yalnızca işletme filtresi
            return reviewRepository
                    .findByBusinessIdOrderByCreatedAtDesc(businessId, pageable)
                    .map(this::toAdminResponse);
        } else if (isVisible != null) {
            // Yalnızca görünürlük filtresi
            return reviewRepository
                    .findByIsVisibleOrderByCreatedAtDesc(isVisible, pageable)
                    .map(this::toAdminResponse);
        } else {
            // Filtre yok — tüm yorumlar
            return reviewRepository
                    .findAllByOrderByCreatedAtDesc(pageable)
                    .map(this::toAdminResponse);
        }
    }

    /**
     * Belirtilen yorumu admin tarafından gizler (isVisible = false).
     * Yorum bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param reviewId Gizlenecek yorumun UUID'si
     */
    @Override
    @Transactional
    public void hideReview(UUID reviewId) {
        log.info("Admin yorum gizleme isteği — reviewId: {}", reviewId);
        Review review = findReviewById(reviewId);
        review.setIsVisible(false);
        reviewRepository.save(review);
        log.info("Yorum gizlendi — reviewId: {}", reviewId);
    }

    /**
     * Gizlenmiş bir yorumu admin tarafından tekrar görünür yapar (isVisible = true).
     * Yorum bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param reviewId Görünür yapılacak yorumun UUID'si
     */
    @Override
    @Transactional
    public void showReview(UUID reviewId) {
        log.info("Admin yorum gösterme isteği — reviewId: {}", reviewId);
        Review review = findReviewById(reviewId);
        review.setIsVisible(true);
        reviewRepository.save(review);
        log.info("Yorum görünür yapıldı — reviewId: {}", reviewId);
    }

    // ── Yardımcı metodlar ────────────────────────────────────────────────────

    /**
     * Kimliğe göre yorumu veri tabanından sorgular.
     * Bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param reviewId Sorgulanacak yorumun UUID'si
     * @return Bulunan Review entity nesnesi
     */
    private Review findReviewById(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorType.REVIEW_NOT_FOUND, "Yorum bulunamadı: " + reviewId));
    }

    /**
     * Review entity nesnesini AdminReviewResponse DTO'suna dönüştürür.
     * İşletme adı ve kullanıcı adı LAZY ilişkilerden @Transactional scope içinde okunur.
     *
     * @param review Dönüştürülecek Review entity nesnesi
     * @return Admin yorum yanıt DTO'su
     */
    private AdminReviewResponse toAdminResponse(Review review) {
        return new AdminReviewResponse(
                review.getId(),
                review.getBusiness().getId(),
                review.getBusiness().getName(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getIsVisible(),
                review.getCreatedAt()
        );
    }
}
