package com.yunus.business.service;

// Sınıf adı: BusinessDiscoveryServiceImpl
// Amacı: BusinessDiscoveryService sözleşmesinin somut implementasyonu.
// Ne yapıyor: Onaylanmış işletmeleri veritabanından çeker, kapak fotoğrafı/
//             ortalama puan/yorum sayısı/kategori gibi ek bilgileri zenginleştirir.
//             lat/lng verilmişse HaversineUtil ile mesafe hesaplar ve sonuçları
//             mesafeye göre artan sırayla döner. Arama ve detay endpoint'lerini
//             de yönetir.

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

    private final BusinessDiscoveryRepository discoveryRepository;
    private final BusinessPhotoRepository     photoRepository;
    private final ServiceRepository           serviceRepository;
    private final StaffRepository             staffRepository;

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
            Pageable pageable) {

        Page<Business> page = discoveryRepository.findApprovedBusinesses(
                categoryId, cityId, districtId, pageable);

        // Her Business'ı yanıt nesnesine dönüştür; koordinat varsa mesafeyi hesapla
        List<BusinessDiscoveryResponse> mapped = page.getContent().stream()
                .map(b -> toDiscoveryResponse(b, lat, lng))
                .collect(Collectors.toList());

        // lat/lng verilmişse distanceKm'e göre artan sıralama uygula
        // (Page'in toplam eleman sayısı ve sayfalama bilgisi korunur)
        if (lat != null && lng != null) {
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
