package com.yunus.business.repository;

// Sınıf adı: BusinessDiscoveryRepository
// Amacı: Keşif ve arama işlemleri için işletme sorgularını barındırır.
// Ne yapıyor: Onaylanmış ve aktif işletmeleri kategori/şehir/ilçe filtresiyle
//             sayfalı olarak listeler; arama, ortalama puan ve yorum sayısı
//             hesaplama sorgularını sağlar. BusinessRepository'den ayrı tutularak
//             sorumluluk ayrımı korunur.

import com.yunus.business.entity.Business;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Keşif ve arama akışlarına özgü işletme repository'si.
 *
 * <p>Bu interface yalnızca kullanıcıya yönelik keşif sorgularını içerir.
 * İşletme sahipliği ve yönetim sorguları {@code BusinessRepository}'de kalır.
 *
 * <p>JOIN FETCH kullanılmaz; tüm ilişkiler lazy-load ile çekilir.
 */
public interface BusinessDiscoveryRepository extends JpaRepository<Business, UUID> {

    // ─────────────────────────────────────────────────────────────────────────
    // findApprovedBusinesses
    // Onaylanmış ve aktif işletmeleri opsiyonel kategori/şehir/ilçe
    // filtresiyle sayfalı olarak döner.
    // null parametre → filtre uygulanmaz (tümü gelir).
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Onaylanmış ({@code APPROVED}) ve aktif ({@code isActive = true}) işletmeleri listeler.
     *
     * @param categoryId  filtre için kategori UUID'si; {@code null} ise kategori filtresi uygulanmaz
     * @param cityId      filtre için şehir UUID'si; {@code null} ise şehir filtresi uygulanmaz
     * @param districtId  filtre için ilçe UUID'si; {@code null} ise ilçe filtresi uygulanmaz
     * @param pageable    sayfalama ve sıralama
     * @return eşleşen işletmelerin sayfası
     */
    @Query("""
            SELECT DISTINCT b
            FROM Business b
            LEFT JOIN b.categories c
            WHERE b.status = com.yunus.business.entity.BusinessStatus.APPROVED
              AND b.isActive = true
              AND (:categoryId IS NULL OR c.id = :categoryId)
              AND (:cityId IS NULL OR b.city.id = :cityId)
              AND (:districtId IS NULL OR b.district.id = :districtId)
            """)
    Page<Business> findApprovedBusinesses(
            @Param("categoryId") UUID categoryId,
            @Param("cityId") UUID cityId,
            @Param("districtId") UUID districtId,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────────────────────
    // findAverageRatingByBusinessId
    // İşletmenin görünür yorumlarının ortalama puanını döner.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Belirtilen işletmenin görünür ({@code isVisible = true}) yorumlarının
     * ortalama puanını döner.
     *
     * @param businessId işletme UUID'si
     * @return ortalama puan; yorum yoksa {@code null}
     */
    @Query("""
            SELECT AVG(r.rating)
            FROM Review r
            WHERE r.business.id = :businessId
              AND r.isVisible = true
            """)
    Double findAverageRatingByBusinessId(@Param("businessId") UUID businessId);

    // ─────────────────────────────────────────────────────────────────────────
    // findReviewCountByBusinessId
    // İşletmenin görünür yorum sayısını döner.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Belirtilen işletmenin görünür ({@code isVisible = true}) yorum sayısını döner.
     *
     * @param businessId işletme UUID'si
     * @return görünür yorum adedi
     */
    @Query("""
            SELECT COUNT(r)
            FROM Review r
            WHERE r.business.id = :businessId
              AND r.isVisible = true
            """)
    Long findReviewCountByBusinessId(@Param("businessId") UUID businessId);

    // ─────────────────────────────────────────────────────────────────────────
    // searchByNameOrDescription
    // Ad veya açıklamada anahtar kelime araması; büyük/küçük harf duyarsız.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Onaylanmış ve aktif işletmeler arasında ad veya açıklamada
     * büyük/küçük harf duyarsız arama yapar.
     *
     * @param query      arama terimi (boşluk bırakılırsa tüm kayıtlar döner)
     * @param cityId     opsiyonel şehir filtresi; {@code null} ise uygulanmaz
     * @param pageable   sayfalama ve sıralama
     * @return eşleşen işletmelerin sayfası
     */
    @Query("""
            SELECT b
            FROM Business b
            WHERE b.status = com.yunus.business.entity.BusinessStatus.APPROVED
              AND b.isActive = true
              AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:cityId IS NULL OR b.city.id = :cityId)
            """)
    Page<Business> searchByNameOrDescription(
            @Param("query") String query,
            @Param("cityId") UUID cityId,
            Pageable pageable
    );
}
