package com.yunus.review.repository;

/**
 * Review entity'si için Spring Data JPA repository arayüzü.
 * <p>
 * Temel CRUD işlemleri JpaRepository tarafından sağlanır.
 * Özel sorgular; işletmeye göre görünür yorumları listeleme,
 * kullanıcıya göre listeleme, randevu tekrarını kontrol etme
 * ve kullanıcıya ait belirli bir yorumu getirme işlevlerini kapsar.
 * </p>
 */

import com.yunus.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Belirtilen işletmeye ait ve görünür (isVisible = true) tüm değerlendirmeleri sayfalı döner.
     *
     * @param businessId Değerlendirmelerin sorgulanacağı işletmenin UUID'si
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return Görünür değerlendirmelerin sayfalı listesi
     */
    Page<Review> findByBusinessIdAndIsVisibleTrue(UUID businessId, Pageable pageable);

    /**
     * Belirtilen kullanıcıya ait tüm değerlendirmeleri sayfalı döner.
     *
     * @param userId   Değerlendirmeleri sorgulanacak kullanıcının UUID'si
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Kullanıcının değerlendirmelerinin sayfalı listesi
     */
    Page<Review> findByUserId(UUID userId, Pageable pageable);

    /**
     * Belirtilen randevuya ait bir değerlendirme olup olmadığını kontrol eder.
     * Aynı randevuya iki kez yorum yapılmasını önlemek için kullanılır.
     *
     * @param appointmentId Kontrol edilecek randevunun UUID'si
     * @return Değerlendirme mevcutsa {@code true}, yoksa {@code false}
     */
    boolean existsByAppointmentId(UUID appointmentId);

    /**
     * Belirtilen değerlendirme kimliği ve kullanıcı kimliğine sahip değerlendirmeyi döner.
     * Kullanıcının yalnızca kendi yorumuna erişmesini garanti eder.
     *
     * @param reviewId Aranacak değerlendirmenin UUID'si
     * @param userId   İşlemi gerçekleştiren kullanıcının UUID'si
     * @return Eşleşen değerlendirme (opsiyonel)
     */
    Optional<Review> findByIdAndUserId(UUID reviewId, UUID userId);
}
