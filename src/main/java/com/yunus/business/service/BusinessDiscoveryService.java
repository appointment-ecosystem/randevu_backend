package com.yunus.business.service;

// Sınıf adı: BusinessDiscoveryService
// Amacı: İşletme keşif ve arama işlemlerinin servis sözleşmesini tanımlar.
// Ne yapıyor: Onaylanmış işletmelerin konum/kategori/şehir/ilçe filtresiyle
//             listelenmesini, detay görüntülenmesini ve anahtar kelime
//             aramasını bildiren üç metod sözleşmesi içerir.

import com.yunus.business.dto.BusinessDiscoveryDetailResponse;
import com.yunus.business.dto.BusinessDiscoveryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * İşletme keşif ve arama akışlarının servis sözleşmesi.
 *
 * <p>Tüm metodlar yalnızca {@code status=APPROVED} ve {@code isActive=true}
 * olan işletmeleri döner.
 */
public interface BusinessDiscoveryService {

    /**
     * Onaylanmış işletmeleri opsiyonel filtrelerle sayfalı olarak döner.
     *
     * <p>{@code lat} ve {@code lng} verilmişse her işletmenin {@code distanceKm}
     * alanı Haversine formülüyle hesaplanır ve sonuçlar mesafeye göre sıralanır.
     * Koordinat verilmemişse {@code distanceKm} {@code null} olarak döner.
     *
     * <p>{@code sortBy="nearest"} isteğinde {@code lat}/{@code lng} eksikse mesafe
     * sıralaması atlanır; varsayılan sıralama (id asc) uygulanır — NPE olmaz.
     *
     * @param categoryId    opsiyonel kategori filtresi; null ise uygulanmaz
     * @param cityId        opsiyonel şehir filtresi; null ise uygulanmaz
     * @param districtId    opsiyonel ilçe filtresi; null ise uygulanmaz
     * @param lat           kullanıcının enlemi (mesafe sıralaması için); null olabilir
     * @param lng           kullanıcının boylamı (mesafe sıralaması için); null olabilir
     * @param sortByNearest true ise mesafeye göre artan sıralama; lat/lng yoksa yoksayılır
     * @param maxDistanceKm maksimum mesafe filtresi (km); null ise uygulanmaz
     * @param onlyOpen      true ise yalnızca şu an açık işletmeleri döner; null/false ise filtre yok
     * @param pageable      sayfalama ve sıralama
     * @return filtrelenmiş işletme özet sayfası
     */
    Page<BusinessDiscoveryResponse> getBusinesses(
            UUID categoryId,
            UUID cityId,
            UUID districtId,
            Double lat,
            Double lng,
            boolean sortByNearest,
            Double maxDistanceKm,
            Boolean onlyOpen,
            Pageable pageable
    );

    /**
     * İşletme detay bilgisini döner.
     *
     * @param id işletme UUID'si
     * @return aktif ve onaylı işletmenin detay yanıtı
     * @throws com.yunus.common.exception.ResourceNotFoundException işletme bulunamazsa
     *         veya aktif/onaylı değilse
     */
    BusinessDiscoveryDetailResponse getBusinessDetail(UUID id);

    /**
     * Onaylanmış işletmeler arasında anahtar kelime araması yapar.
     *
     * <p>Ad veya açıklama alanında büyük/küçük harf duyarsız ILIKE araması yapılır.
     * {@code distanceKm} bu endpoint'te her zaman {@code null} döner.
     *
     * @param q        arama terimi
     * @param cityId   opsiyonel şehir filtresi; null ise uygulanmaz
     * @param pageable sayfalama ve sıralama
     * @return eşleşen işletme özet sayfası
     */
    Page<BusinessDiscoveryResponse> searchBusinesses(
            String q,
            UUID cityId,
            Pageable pageable
    );
}
