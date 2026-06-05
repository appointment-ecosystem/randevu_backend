package com.yunus.business.controller;

// Sınıf adı: DiscoverController
// Amacı: İşletme keşif ve arama endpoint'lerini dışarıya açar.
// Ne yapıyor: Onaylanmış işletmeleri kategori/şehir/ilçe/konum filtresiyle
//             listeler, detayını döner ve anahtar kelime araması sağlar.
//             Tüm endpoint'ler public'tir; güvenlik ayarı P7 fazında yapılacak.

import com.yunus.business.dto.BusinessDiscoveryDetailResponse;
import com.yunus.business.dto.BusinessDiscoveryResponse;
import com.yunus.business.dto.OpenStatusResponse;
import com.yunus.business.service.BusinessDiscoveryService;
import com.yunus.business.service.OpenStatusService;
import com.yunus.common.response.BaseResponse;
import com.yunus.ratelimit.annotation.KeyType;
import com.yunus.ratelimit.annotation.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşletme keşif ve arama endpoint'leri.
 *
 * <p>Base path: {@code /api/v1/discover}
 * <p>Tüm endpoint'ler kimlik doğrulaması gerektirmez; P7 fazında güvenlik konfigürasyonu eklenecek.
 */
@RestController
@RequestMapping("/api/v1/discover")
@Tag(name = "Discover", description = "Onaylanmış işletmeleri keşfet ve ara")
@RequiredArgsConstructor
public class DiscoverController {

    private final BusinessDiscoveryService businessDiscoveryService;
    private final OpenStatusService openStatusService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/discover/businesses
    // Onaylanmış işletmeleri opsiyonel filtre ve koordinat ile listeler.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Onaylanmış ve aktif işletmeleri listeler.
     *
     * <p>Opsiyonel filtreler: {@code categoryId}, {@code cityId}, {@code districtId}.
     * {@code lat} ve {@code lng} verilirse sonuçlar kullanıcıya olan mesafeye göre artan sırada döner.
     *
     * <p>GET /api/v1/discover/businesses
     */
    // Keşif listesi için 1 dakikada IP başına en fazla 100 istek
    @RateLimit(limit = 100, windowSeconds = 60, key = "discover", keyType = KeyType.IP)
    @GetMapping("/businesses")
    @Operation(
            summary = "İşletme listesi",
            description = "Onaylanmış işletmeleri filtreler ve opsiyonel olarak konuma göre sıralar."
    )
    public ResponseEntity<BaseResponse<Page<BusinessDiscoveryResponse>>> getBusinesses(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BusinessDiscoveryResponse> result = businessDiscoveryService
                .getBusinesses(categoryId, cityId, districtId, lat, lng,
                        lat != null && lng != null,   // sortByNearest: koordinat varsa mesafeye göre sırala
                        null,                          // maxDistanceKm: discover endpoint'te filtre yok
                        null,                          // onlyOpen: discover endpoint'te filtre yok
                        pageable);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/discover/businesses/search
    // Anahtar kelimeyle işletme arar — /search, /{id}'den önce tanımlanmalı
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * İşletme adı veya açıklamasında anahtar kelime araması yapar.
     *
     * <p>{@code q} parametresi büyük/küçük harf duyarsız olarak name ve description alanlarında aranır.
     *
     * <p>GET /api/v1/discover/businesses/search
     */
    // Arama endpoint'i için 1 dakikada IP başına en fazla 100 istek
    @RateLimit(limit = 100, windowSeconds = 60, key = "discover", keyType = KeyType.IP)
    @GetMapping("/businesses/search")
    @Operation(
            summary = "İşletme arama",
            description = "Ad veya açıklamada büyük/küçük harf duyarsız arama yapar."
    )
    public ResponseEntity<BaseResponse<Page<BusinessDiscoveryResponse>>> searchBusinesses(
            @RequestParam String q,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BusinessDiscoveryResponse> result = businessDiscoveryService
                .searchBusinesses(q, cityId, pageable);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/discover/businesses/{id}
    // Tek işletmenin detay bilgisini döner.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Belirtilen işletmenin detay bilgisini döner.
     *
     * <p>İşletme bulunamazsa veya aktif/onaylı değilse {@code 404 Not Found} döner.
     *
     * <p>GET /api/v1/discover/businesses/{id}
     */
    // İşletme detayı için 1 dakikada IP başına en fazla 100 istek
    @RateLimit(limit = 100, windowSeconds = 60, key = "discover", keyType = KeyType.IP)
    @GetMapping("/businesses/{id}")
    @Operation(
            summary = "İşletme detayı",
            description = "Onaylanmış ve aktif işletmenin hizmet, personel ve iletişim bilgilerini döner."
    )
    public ResponseEntity<BaseResponse<BusinessDiscoveryDetailResponse>> getBusinessDetail(
            @PathVariable UUID id) {

        BusinessDiscoveryDetailResponse result = businessDiscoveryService.getBusinessDetail(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/discover/businesses/{id}/open-status
    // İşletmenin anlık açık/kapalı durumunu döner.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Belirtilen işletmenin anlık açık/kapalı durumunu döner.
     *
     * <p>GET /api/v1/discover/businesses/{id}/open-status
     */
    // İşletme çalışma durumu için 1 dakikada IP başına en fazla 100 istek
    @RateLimit(limit = 100, windowSeconds = 60, key = "discover", keyType = KeyType.IP)
    @GetMapping("/businesses/{id}/open-status")
    @Operation(
            summary = "İşletme açık/kapalı durumu",
            description = "Onaylanmış işletmenin o an açık veya kapalı olma durumunu detaylarıyla döner."
    )
    public ResponseEntity<BaseResponse<OpenStatusResponse>> getOpenStatus(@PathVariable UUID id) {
        OpenStatusResponse result = openStatusService.getOpenStatus(id);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
