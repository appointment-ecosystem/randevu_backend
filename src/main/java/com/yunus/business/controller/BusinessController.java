package com.yunus.business.controller;

import com.yunus.business.dto.BusinessResponse;
import com.yunus.business.dto.BusinessStatusResponse;
import com.yunus.business.dto.CreateBusinessRequest;
import com.yunus.business.dto.UpdateBusinessRequest;
import com.yunus.business.service.BusinessService;
import com.yunus.common.response.BaseResponse;
import com.yunus.security.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşletme oluşturma ve yönetim endpoint'leri.
 */
@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final CurrentUserService currentUserService;

    public BusinessController(BusinessService businessService, CurrentUserService currentUserService) {
        this.businessService = businessService;
        this.currentUserService = currentUserService;
    }

    /**
     * İşletme oluşturur (sadece BUSINESS_OWNER).
     * POST /api/v1/businesses
     */
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

