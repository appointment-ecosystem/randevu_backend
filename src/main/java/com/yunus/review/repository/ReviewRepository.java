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

    // ── Admin yorum yönetimi sorguları ─────────────────────────────────────────

    /**
     * Tüm yorumları oluşturulma tarihine göre azalan sırada sayfalı döner.
     * Admin panelinde filtre uygulanmadan tüm yorumları listelemek için kullanılır.
     *
     * @param pageable Sayfalama ve sıralama bilgisi
     * @return Tüm yorumların sayfalı listesi, {@code createdAt DESC} sıralı
     */
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Belirtilen işletmeye ait tüm yorumları (görünür/gizli ayrımı yapmadan)
     * oluşturulma tarihine göre azalan sırada sayfalı döner.
     *
     * @param businessId Filtrelenecek işletmenin UUID'si
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return İşletmeye ait yorumların sayfalı listesi, {@code createdAt DESC} sıralı
     */
    Page<Review> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    /**
     * Görünürlük durumuna göre filtreli yorumları oluşturulma tarihine göre
     * azalan sırada sayfalı döner.
     *
     * @param isVisible Görünürlük filtresi (true = görünür, false = gizli)
     * @param pageable  Sayfalama ve sıralama bilgisi
     * @return Filtrelenmiş yorumların sayfalı listesi, {@code createdAt DESC} sıralı
     */
    Page<Review> findByIsVisibleOrderByCreatedAtDesc(Boolean isVisible, Pageable pageable);

    /**
     * Hem işletme hem görünürlük durumuna göre filtrelenmiş yorumları
     * oluşturulma tarihine göre azalan sırada sayfalı döner.
     *
     * @param businessId Filtrelenecek işletmenin UUID'si
     * @param isVisible  Görünürlük filtresi (true = görünür, false = gizli)
     * @param pageable   Sayfalama ve sıralama bilgisi
     * @return İkili filtreye uyan yorumların sayfalı listesi, {@code createdAt DESC} sıralı
     */
    Page<Review> findByBusinessIdAndIsVisibleOrderByCreatedAtDesc(UUID businessId, Boolean isVisible, Pageable pageable);
}

