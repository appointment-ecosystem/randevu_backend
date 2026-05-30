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
import com.yunus.review.dto.CreateReviewRequest;
import com.yunus.review.dto.ReviewResponse;
import com.yunus.review.entity.Review;
import com.yunus.review.repository.ReviewRepository;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

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
}
