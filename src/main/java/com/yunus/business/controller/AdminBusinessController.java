package com.yunus.business.controller;

import com.yunus.business.dto.AdminBusinessDetailResponse;
import com.yunus.business.dto.AdminBusinessListResponse;
import com.yunus.business.dto.AdminBusinessStatusUpdateRequest;
import com.yunus.business.entity.BusinessStatus;
import com.yunus.business.service.AdminBusinessService;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin rolüne sahip kullanıcıların işletmeleri onaylama, reddetme, askıya alma,
 * aktifleştirme ve listeleme işlemlerini yapabilmesini sağlayan denetleyici sınıf.
 */
@RestController
@RequestMapping("/api/v1/admin/businesses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBusinessController {

    private final AdminBusinessService adminBusinessService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param adminBusinessService Admin işletme yönetim servisi
     */
    public AdminBusinessController(AdminBusinessService adminBusinessService) {
        this.adminBusinessService = adminBusinessService;
    }

    /**
     * Sistemdeki tüm işletmeleri (opsiyonel onay durumu filtresi ile) sayfalanmış olarak getirir.
     * GET /api/v1/admin/businesses
     *
     * @param status Filtrelenecek işletme durumu (isteğe bağlı)
     * @param page Sayfa numarası (varsayılan 0)
     * @param size Sayfadaki kayıt sayısı (varsayılan 20)
     * @return Sayfalanmış işletme liste yanıtı
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<AdminBusinessListResponse>>> getAllBusinesses(
            @RequestParam(required = false) BusinessStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminBusinessListResponse> response = adminBusinessService.getAllBusinesses(status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * ID'si verilen işletmenin admin paneline özel tüm detay bilgilerini getirir.
     * GET /api/v1/admin/businesses/{id}
     *
     * @param id İşletme kimliği
     * @return İşletme detay yanıtı
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AdminBusinessDetailResponse>> getBusinessDetail(@PathVariable UUID id) {
        AdminBusinessDetailResponse response = adminBusinessService.getBusinessDetail(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen işletmeyi onaylar.
     * PATCH /api/v1/admin/businesses/{id}/approve
     *
     * @param id İşletme kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<BaseResponse<Void>> approveBusiness(@PathVariable UUID id) {
        adminBusinessService.approveBusiness(id);
        return ResponseEntity.ok(BaseResponse.success("İşletme başarıyla onaylandı"));
    }

    /**
     * Belirtilen işletmeyi red gerekçesiyle birlikte reddeder.
     * PATCH /api/v1/admin/businesses/{id}/reject
     *
     * @param id İşletme kimliği
     * @param request Red gerekçesini içeren istek nesnesi
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<BaseResponse<Void>> rejectBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody AdminBusinessStatusUpdateRequest request) {
        adminBusinessService.rejectBusiness(id, request.reason());
        return ResponseEntity.ok(BaseResponse.success("İşletme reddedildi"));
    }

    /**
     * Belirtilen işletmeyi gerekçe ile askıya alır.
     * PATCH /api/v1/admin/businesses/{id}/suspend
     *
     * @param id İşletme kimliği
     * @param request Askıya alma gerekçesini içeren istek nesnesi
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<BaseResponse<Void>> suspendBusiness(
            @PathVariable UUID id,
            @Valid @RequestBody AdminBusinessStatusUpdateRequest request) {
        adminBusinessService.suspendBusiness(id, request.reason());
        return ResponseEntity.ok(BaseResponse.success("İşletme askıya alındı"));
    }

    /**
     * Pasif veya askıdaki işletmeyi yeniden aktif hale getirir (durumunu APPROVED yapar).
     * PATCH /api/v1/admin/businesses/{id}/activate
     *
     * @param id İşletme kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<BaseResponse<Void>> activateBusiness(@PathVariable UUID id) {
        adminBusinessService.activateBusiness(id);
        return ResponseEntity.ok(BaseResponse.success("İşletme aktif edildi"));
    }
}
