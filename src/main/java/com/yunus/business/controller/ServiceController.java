package com.yunus.business.controller;

import com.yunus.business.dto.CreateServiceRequest;
import com.yunus.business.dto.ServiceResponse;
import com.yunus.business.dto.UpdateServiceRequest;
import com.yunus.business.service.ServiceManagementService;
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
 * İşletme hizmetleri CRUD ve görsel yönetimi endpoint'leri.
 * Yetki kontrolü service katmanında yapılır; controller yalnızca yönlendirme sağlar.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    /**
     * İşletmenin aktif hizmet listesini döner.
     * GET /api/v1/businesses/{businessId}/services
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<ServiceResponse>>> getServices(
            @PathVariable UUID businessId) {
        List<ServiceResponse> services = serviceManagementService.getServices(businessId);
        return ResponseEntity.ok(BaseResponse.success(services));
    }

    /**
     * Tek bir hizmetin detayını döner.
     * GET /api/v1/businesses/{businessId}/services/{serviceId}
     */
    @GetMapping("/{serviceId}")
    public ResponseEntity<BaseResponse<ServiceResponse>> getService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId) {
        ServiceResponse response = serviceManagementService.getService(businessId, serviceId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Yeni hizmet oluşturur; yalnızca işletme sahibi yapabilir.
     * POST /api/v1/businesses/{businessId}/services
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> createService(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceResponse response = serviceManagementService.createService(businessId, request);
        return ResponseEntity.ok(BaseResponse.success("Hizmet başarıyla oluşturuldu", response));
    }

    /**
     * Mevcut hizmeti günceller; görsel alanına dokunmaz.
     * PUT /api/v1/businesses/{businessId}/services/{serviceId}
     */
    @PutMapping("/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> updateService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpdateServiceRequest request) {
        ServiceResponse response = serviceManagementService.updateService(businessId, serviceId, request);
        return ResponseEntity.ok(BaseResponse.success("Hizmet başarıyla güncellendi", response));
    }

    /**
     * Hizmet görseli yükler; varsa eski görsel R2'den silinir.
     * POST /api/v1/businesses/{businessId}/services/{serviceId}/image
     */
    @PostMapping(value = "/{serviceId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> uploadServiceImage(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @RequestParam("file") MultipartFile file) {
        ServiceResponse response = serviceManagementService.uploadServiceImage(businessId, serviceId, file);
        return ResponseEntity.ok(BaseResponse.success("Hizmet görseli yüklendi", response));
    }

    /**
     * Hizmet görselini R2'den ve DB'den siler.
     * DELETE /api/v1/businesses/{businessId}/services/{serviceId}/image
     */
    @DeleteMapping("/{serviceId}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> deleteServiceImage(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId) {
        ServiceResponse response = serviceManagementService.deleteServiceImage(businessId, serviceId);
        return ResponseEntity.ok(BaseResponse.success("Hizmet görseli silindi", response));
    }

    /**
     * Hizmeti pasife alır; fiziksel silme yapmaz.
     * PATCH /api/v1/businesses/{businessId}/services/{serviceId}/deactivate
     */
    @PatchMapping("/{serviceId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> deactivateService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId) {
        ServiceResponse response = serviceManagementService.deactivateService(businessId, serviceId);
        return ResponseEntity.ok(BaseResponse.success("Hizmet pasife alındı", response));
    }

    /**
     * Pasif hizmeti tekrar aktive eder.
     * PATCH /api/v1/businesses/{businessId}/services/{serviceId}/activate
     */
    @PatchMapping("/{serviceId}/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<ServiceResponse>> activateService(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId) {
        ServiceResponse response = serviceManagementService.activateService(businessId, serviceId);
        return ResponseEntity.ok(BaseResponse.success("Hizmet aktive edildi", response));
    }
}
