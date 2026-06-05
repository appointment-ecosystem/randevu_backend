package com.yunus.business.controller;

import com.yunus.business.dto.BusinessPhotoResponse;
import com.yunus.business.dto.PhotoSortRequest;
import com.yunus.business.service.BusinessPhotoService;
import com.yunus.common.response.BaseResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme fotoğraf yönetimi endpoint'leri.
 * Sahip kontrolü service katmanında yapılır; controller yalnızca yönlendirme sağlar.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/photos")
public class BusinessPhotoController {

    private final BusinessPhotoService businessPhotoService;

    public BusinessPhotoController(BusinessPhotoService businessPhotoService) {
        this.businessPhotoService = businessPhotoService;
    }

    /**
     * İşletmenin fotoğraf listesini sortOrder'a göre sıralı döner.
     * GET /api/v1/businesses/{businessId}/photos
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<BusinessPhotoResponse>>> getPhotos(
            @PathVariable UUID businessId) {
        List<BusinessPhotoResponse> photos = businessPhotoService.getPhotos(businessId);
        return ResponseEntity.ok(BaseResponse.success(photos));
    }

    /**
     * İşletmeye yeni fotoğraf yükler; multipart/form-data ile dosya alınır.
     * POST /api/v1/businesses/{businessId}/photos
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<BusinessPhotoResponse>> uploadPhoto(
            @PathVariable UUID businessId,
            @RequestParam("file") MultipartFile file) {
        BusinessPhotoResponse response = businessPhotoService.uploadPhoto(businessId, file);
        return ResponseEntity.ok(BaseResponse.success("Fotoğraf başarıyla yüklendi", response));
    }

    /**
     * Belirtilen fotoğrafı kapak görseli yapar; önceki kapak otomatik kaldırılır.
     * PATCH /api/v1/businesses/{businessId}/photos/{photoId}/cover
     */
    @PatchMapping("/{photoId}/cover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<BusinessPhotoResponse>> setCover(
            @PathVariable UUID businessId,
            @PathVariable UUID photoId) {
        BusinessPhotoResponse response = businessPhotoService.setCover(businessId, photoId);
        return ResponseEntity.ok(BaseResponse.success("Kapak fotoğrafı güncellendi", response));
    }

    /**
     * Fotoğrafı R2'den ve DB'den siler; geri alınamaz.
     * DELETE /api/v1/businesses/{businessId}/photos/{photoId}
     */
    @DeleteMapping("/{photoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deletePhoto(
            @PathVariable UUID businessId,
            @PathVariable UUID photoId) {
        businessPhotoService.deletePhoto(businessId, photoId);
        return ResponseEntity.ok(BaseResponse.success("Fotoğraf silindi"));
    }

    /**
     * Birden fazla fotoğrafın sıralama değerini toplu günceller.
     * PATCH /api/v1/businesses/{businessId}/photos/sort
     */
    @PatchMapping("/sort")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<BusinessPhotoResponse>>> updateSortOrder(
            @PathVariable UUID businessId,
            @RequestBody List<PhotoSortRequest> requests) {
        List<BusinessPhotoResponse> photos = businessPhotoService.updateSortOrder(businessId, requests);
        return ResponseEntity.ok(BaseResponse.success("Sıralama güncellendi", photos));
    }
}
