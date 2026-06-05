package com.yunus.business.controller;

import com.yunus.business.dto.BusinessDiscoveryResponse;
import com.yunus.business.dto.BusinessResponse;
import com.yunus.business.dto.BusinessStatusResponse;
import com.yunus.business.dto.CreateBusinessRequest;
import com.yunus.business.dto.UpdateBusinessRequest;
import com.yunus.business.service.BusinessDiscoveryService;
import com.yunus.business.service.BusinessService;
import com.yunus.common.response.BaseResponse;
import com.yunus.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşletme oluşturma ve yönetim endpoint'leri.
 */
@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessDiscoveryService businessDiscoveryService;
    private final CurrentUserService currentUserService;

    public BusinessController(BusinessService businessService,
                              BusinessDiscoveryService businessDiscoveryService,
                              CurrentUserService currentUserService) {
        this.businessService = businessService;
        this.businessDiscoveryService = businessDiscoveryService;
        this.currentUserService = currentUserService;
    }

    /**
     * Onaylanmış işletmeleri listeler (public).
     * GET /api/v1/businesses
     *
     * <p>Desteklenen query parametreleri:
     * <ul>
     *   <li>{@code sortBy} — "nearest" verilirse konuma göre sırala;
     *       lat/lng eksikse NPE yerine varsayılan sıralama uygulanır.</li>
     *   <li>{@code lat}, {@code lng} — kullanıcı koordinatları (mesafe hesabı için).</li>
     *   <li>{@code maxDistance} — km cinsinden maksimum mesafe filtresi.</li>
     *   <li>{@code onlyOpen} — true ise yalnızca şu an açık işletmeler.</li>
     *   <li>{@code categoryId}, {@code cityId}, {@code districtId} — opsiyonel filtreler.</li>
     *   <li>{@code page} (0-indexed), {@code size} — sayfalama.</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<BusinessDiscoveryResponse>>> getBusinesses(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Boolean onlyOpen,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        boolean sortByNearest = "nearest".equalsIgnoreCase(sortBy);
        Pageable pageable = PageRequest.of(page, size);

        Page<BusinessDiscoveryResponse> result = businessDiscoveryService.getBusinesses(
                categoryId, cityId, districtId,
                lat, lng,
                sortByNearest,
                maxDistance,
                onlyOpen,
                pageable);

        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<BaseResponse<BusinessResponse>> createBusiness(
            @Valid @RequestBody CreateBusinessRequest request) {
        UUID ownerId = currentUserService.getCurrentUserId();
        BusinessResponse response = businessService.createBusiness(ownerId, request);
        return ResponseEntity.ok(BaseResponse.success("İşletme başarıyla oluşturuldu", response));
    }

    /**
     * Giriş yapan kullanıcının kendi işletmelerini listeler.
     * GET /api/v1/businesses/mine
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','BUSINESS_EMPLOYEE','ADMIN','USER')")
    public ResponseEntity<BaseResponse<List<BusinessResponse>>> getMyBusinesses() {
        UUID ownerId = currentUserService.getCurrentUserId();
        List<BusinessResponse> businesses = businessService.getMyBusinesses(ownerId);
        return ResponseEntity.ok(BaseResponse.success(businesses));
    }

    /**
     * İşletme detayını döner (public).
     * GET /api/v1/businesses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BusinessResponse>> getBusiness(@PathVariable UUID id) {
        BusinessResponse response = businessService.getBusiness(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Kendi işletmesini günceller.
     * PATCH /api/v1/businesses/{id}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BUSINESS_OWNER')")
    public ResponseEntity<BaseResponse<BusinessResponse>> updateBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBusinessRequest request) {
        UUID ownerId = currentUserService.getCurrentUserId();
        BusinessResponse response = businessService.updateBusiness(ownerId, id, request);
        return ResponseEntity.ok(BaseResponse.success("İşletme başarıyla güncellendi", response));
    }

    /**
     * Kendi işletmesinin durumunu döner.
     * GET /api/v1/businesses/{id}/status
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
    public ResponseEntity<BaseResponse<BusinessStatusResponse>> getMyBusinessStatus(@PathVariable UUID id) {
        UUID ownerId = currentUserService.getCurrentUserId();
        BusinessStatusResponse status = businessService.getMyBusinessStatus(ownerId, id);
        return ResponseEntity.ok(BaseResponse.success(status));
    }
}

