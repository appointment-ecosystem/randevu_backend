package com.yunus.business.service;

// Sınıf adı: BusinessDiscoveryServiceImpl
// Amacı: BusinessDiscoveryService sözleşmesinin somut implementasyonu.
// Ne yapıyor: Onaylanmış işletmeleri veritabanından çeker, kapak fotoğrafı/
//             ortalama puan/yorum sayısı/kategori gibi ek bilgileri zenginleştirir.
//             lat/lng verilmişse HaversineUtil ile mesafe hesaplar ve sonuçları
//             mesafeye göre artan sırayla döner. sortByNearest=true ama lat/lng
//             eksikse NPE yerine varsayılan sıralama uygulanır. onlyOpen ve
//             maxDistanceKm filtreleri de desteklenir.

import com.yunus.business.dto.BusinessDiscoveryDetailResponse;
import com.yunus.business.dto.BusinessDiscoveryDetailResponse.ServiceSummary;
import com.yunus.business.dto.BusinessDiscoveryDetailResponse.StaffSummary;
import com.yunus.business.dto.BusinessDiscoveryResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessCategory;
import com.yunus.business.entity.BusinessStatus;
import com.yunus.business.repository.BusinessDiscoveryRepository;
import com.yunus.business.repository.BusinessPhotoRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.common.util.HaversineUtil;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link BusinessDiscoveryService} implementasyonu.
 *
 * <p>Tüm okuma işlemleri {@code @Transactional(readOnly = true)} ile korunur.
 * Fotoğraf, hizmet ve personel verileri ayrı repository'lerden lazy olarak çekilir.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessDiscoveryServiceImpl implements BusinessDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(BusinessDiscoveryServiceImpl.class);

    private final BusinessDiscoveryRepository discoveryRepository;
    private final BusinessPhotoRepository     photoRepository;
    private final ServiceRepository           serviceRepository;
    private final StaffRepository             staffRepository;
    private final OpenStatusService           openStatusService;

    // ─────────────────────────────────────────────────────────────────────────
    // getBusinesses
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<BusinessDiscoveryResponse> getBusinesses(
            UUID categoryId,
            UUID cityId,
            UUID districtId,
            Double lat,
            Double lng,
            boolean sortByNearest,
            Double maxDistanceKm,
            Boolean onlyOpen,
            Pageable pageable) {

        // sortByNearest=true ama koordinat verilmemişse güvenli fallback
        // — NPE yerine loglayarak varsayılan sıralamaya geç
        final boolean canSortByDistance = sortByNearest && lat != null && lng != null;
        if (sortByNearest && (lat == null || lng == null)) {
            log.warn("sortBy=nearest istendi fakat lat/lng parametresi eksik; "
                    + "varsayılan sıralama uygulanıyor");
        }

        Page<Business> page = discoveryRepository.findApprovedBusinesses(
                categoryId, cityId, districtId, pageable);

        // Her Business'ı yanıt nesnesine dönüştür; koordinat varsa mesafeyi hesapla
        List<BusinessDiscoveryResponse> mapped = page.getContent().stream()
                .map(b -> toDiscoveryResponse(b, lat, lng))
                // maxDistanceKm filtresi: koordinat ve eşik varsa uygula
                .filter(r -> {
                    if (maxDistanceKm == null || lat == null || lng == null) return true;
                    return r.distanceKm() != null && r.distanceKm() <= maxDistanceKm;
                })
                // onlyOpen filtresi: true ise şu an kapalı işletmeleri ele
                .filter(r -> {
                    if (!Boolean.TRUE.equals(onlyOpen)) return true;
                    try {
                        return openStatusService.getOpenStatus(r.id()).open();
                    } catch (Exception e) {
                        // işletme bulunamazsa veya durum hesaplanamazsa dahil et
                        log.debug("Open-status kontrolü başarısız, işletme dahil ediliyor: {}", r.id());
                        return true;
                    }
                })
                .collect(Collectors.toList());

        // Mesafeye göre artan sıralama — yalnızca koordinat tamam olduğunda
        if (canSortByDistance) {
            mapped.sort(Comparator.comparingDouble(r ->
                    r.distanceKm() != null ? r.distanceKm() : Double.MAX_VALUE));
        }

        return new PageImpl<>(mapped, page.getPageable(), page.getTotalElements());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBusinessDetail
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public BusinessDiscoveryDetailResponse getBusinessDetail(UUID id) {

        Business business = discoveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", id));

        // Onaylı ve aktif olmayan işletme public API'de bulunamaz
        if (business.getStatus() != BusinessStatus.APPROVED
                || !Boolean.TRUE.equals(business.getIsActive())) {
            throw new ResourceNotFoundException("İşletme", "id", id);
        }

        // Aktif hizmetler — sortOrder ile sıralı (repository seviyesinde ORDER BY)
        List<ServiceSummary> services = serviceRepository
                .findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(business.getId())
                .stream()
                .map(s -> new ServiceSummary(
                        s.getId(),
                        s.getName(),
                        s.getDurationMin(),
                        s.getPrice(),
                        s.getCurrency(),
                        s.getImageUrl()
                ))
                .toList();

        // Aktif personel — sortOrder ile sıralı (repository seviyesinde ORDER BY)
        List<StaffSummary> staffList = staffRepository
                .findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(business.getId())
                .stream()
                .map(s -> new StaffSummary(
                        s.getId(),
                        s.getFullName(),
                        s.getTitle(),
                        s.getProfilePhotoUrl()
                ))
                .toList();

        // Kapak fotoğrafı — isCover=true olan ilk kayıt; yoksa null
        String coverUrl = photoRepository
                .findByBusinessIdAndIsCoverTrue(business.getId())
                .map(p -> p.getUrl())
                .orElse(null);

        Double avgRating  = discoveryRepository.findAverageRatingByBusinessId(business.getId());
        Long   reviewCount = discoveryRepository.findReviewCountByBusinessId(business.getId());

        List<String> categoryNames = business.getCategories().stream()
                .map(BusinessCategory::getName)
                .toList();

        var city  = business.getCity();
        var dist  = business.getDistrict();
        var neigh = business.getNeighborhood();

        return new BusinessDiscoveryDetailResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                business.getDescription(),
                business.getPhone(),
                business.getEmail(),
                business.getWebsite(),
                city  != null ? city.getName()  : null,
                dist  != null ? dist.getName()  : null,
                neigh != null ? neigh.getName() : null,
                business.getAddressLine(),
                coverUrl,
                avgRating,
                reviewCount != null ? reviewCount : 0L,
                business.getLatitude(),
                business.getLongitude(),
                null,       // distanceKm — detay endpoint'inde koordinat alınmıyor
                categoryNames,
                services,
                staffList
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // searchBusinesses
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<BusinessDiscoveryResponse> searchBusinesses(
            String q,
            UUID cityId,
            Pageable pageable) {

        Page<Business> page = discoveryRepository.searchByNameOrDescription(q, cityId, pageable);

        // Arama sonuçlarında kullanıcı koordinatı olmadığından distanceKm null bırakılır
        List<BusinessDiscoveryResponse> mapped = page.getContent().stream()
                .map(b -> toDiscoveryResponse(b, null, null))
                .toList();

        return new PageImpl<>(mapped, page.getPageable(), page.getTotalElements());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Yardımcı metodlar
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bir {@link Business}'ı {@link BusinessDiscoveryResponse}'a dönüştürür.
     *
     * <p>Koordinat çifti (userLat, userLng) her ikisi de non-null ise ve
     * işletmenin kendi koordinatı da mevcutsa Haversine ile mesafe hesaplanır;
     * aksi hâlde {@code distanceKm} null bırakılır.
     */
    private BusinessDiscoveryResponse toDiscoveryResponse(Business business,
                                                          Double userLat,
                                                          Double userLng) {
        // Kapak fotoğrafı
        String coverUrl = photoRepository
                .findByBusinessIdAndIsCoverTrue(business.getId())
                .map(p -> p.getUrl())
                .orElse(null);

        Double avgRating   = discoveryRepository.findAverageRatingByBusinessId(business.getId());
        Long   reviewCount = discoveryRepository.findReviewCountByBusinessId(business.getId());

        List<String> categoryNames = business.getCategories().stream()
                .map(BusinessCategory::getName)
                .toList();

        // Mesafe hesaplama — her iki tarafta da koordinat olmak zorunda
        Double distanceKm = null;
        if (userLat != null && userLng != null
                && business.getLatitude() != null && business.getLongitude() != null) {
            distanceKm = HaversineUtil.calculate(
                    userLat, userLng,
                    business.getLatitude(), business.getLongitude()
            );
        }

        var city = business.getCity();
        var dist = business.getDistrict();

        return new BusinessDiscoveryResponse(
                business.getId(),
                business.getName(),
                business.getSlug(),
                business.getDescription(),
                city != null ? city.getName() : null,
                dist != null ? dist.getName() : null,
                coverUrl,
                avgRating,
                reviewCount != null ? reviewCount : 0L,
                business.getLatitude(),
                business.getLongitude(),
                distanceKm,
                categoryNames
        );
    }
}
