package com.yunus.business.controller;

import com.yunus.business.dto.CreateStaffRequest;
import com.yunus.business.dto.StaffResponse;
import com.yunus.business.dto.UpdateStaffRequest;
import com.yunus.business.service.StaffService;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme personeli yönetimi endpoint'leri.
 * Yetki ve sahip kontrolü service katmanında yapılır; controller yalnızca yönlendirme sağlar.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * İşletmenin aktif personel listesini döner.
     * GET /api/v1/businesses/{businessId}/staff
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<StaffResponse>>> getStaffList(
            @PathVariable UUID businessId) {
        List<StaffResponse> staffList = staffService.getStaffList(businessId);
        return ResponseEntity.ok(BaseResponse.success(staffList));
    }

    /**
     * Tek bir personelin detayını döner.
     * GET /api/v1/businesses/{businessId}/staff/{staffId}
     */
    @GetMapping("/{staffId}")
    public ResponseEntity<BaseResponse<StaffResponse>> getStaff(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId) {
        StaffResponse response = staffService.getStaff(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Yeni personel oluşturur; yalnızca işletme sahibi yapabilir.
     * POST /api/v1/businesses/{businessId}/staff
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> createStaff(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateStaffRequest request) {
        StaffResponse response = staffService.createStaff(businessId, request);
        return ResponseEntity.ok(BaseResponse.success("Personel başarıyla oluşturuldu", response));
    }

    /**
     * Personel bilgisini günceller; fotoğraf ve servisler bu endpoint'ten değişmez.
     * PUT /api/v1/businesses/{businessId}/staff/{staffId}
     */
    @PutMapping("/{staffId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> updateStaff(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId,
            @Valid @RequestBody UpdateStaffRequest request) {
        StaffResponse response = staffService.updateStaff(businessId, staffId, request);
        return ResponseEntity.ok(BaseResponse.success("Personel başarıyla güncellendi", response));
    }

    /**
     * Profil fotoğrafı yükler; varsa eski fotoğraf R2'den silinir.
     * POST /api/v1/businesses/{businessId}/staff/{staffId}/photo
     */
    @PostMapping(value = "/{staffId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> uploadProfilePhoto(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId,
            @RequestParam("file") MultipartFile file) {
        StaffResponse response = staffService.uploadProfilePhoto(businessId, staffId, file);
        return ResponseEntity.ok(BaseResponse.success("Profil fotoğrafı yüklendi", response));
    }

    /**
     * Profil fotoğrafını R2'den ve DB'den siler.
     * DELETE /api/v1/businesses/{businessId}/staff/{staffId}/photo
     */
    @DeleteMapping("/{staffId}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> deleteProfilePhoto(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId) {
        StaffResponse response = staffService.deleteProfilePhoto(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success("Profil fotoğrafı silindi", response));
    }

    /**
     * Personeli pasife alır; fiziksel silme yapmaz.
     * PATCH /api/v1/businesses/{businessId}/staff/{staffId}/deactivate
     */
    @PatchMapping("/{staffId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> deactivateStaff(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId) {
        StaffResponse response = staffService.deactivateStaff(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success("Personel pasife alındı", response));
    }

    /**
     * Pasif personeli tekrar aktive eder.
     * PATCH /api/v1/businesses/{businessId}/staff/{staffId}/activate
     */
    @PatchMapping("/{staffId}/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> activateStaff(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId) {
        StaffResponse response = staffService.activateStaff(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success("Personel aktive edildi", response));
    }

    /**
     * Personele hizmet atar; body olarak UUID listesi alır.
     * POST /api/v1/businesses/{businessId}/staff/{staffId}/services
     */
    @PostMapping("/{staffId}/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> assignServices(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId,
            @RequestBody List<UUID> serviceIds) {
        StaffResponse response = staffService.assignServices(businessId, staffId, serviceIds);
        return ResponseEntity.ok(BaseResponse.success("Hizmetler personele atandı", response));
    }

    /**
     * Personelden belirtilen hizmetleri kaldırır; body olarak UUID listesi alır.
     * DELETE /api/v1/businesses/{businessId}/staff/{staffId}/services
     */
    @DeleteMapping("/{staffId}/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StaffResponse>> removeServices(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId,
            @RequestBody List<UUID> serviceIds) {
        StaffResponse response = staffService.removeServices(businessId, staffId, serviceIds);
        return ResponseEntity.ok(BaseResponse.success("Hizmetler personelden kaldırıldı", response));
    }
}
