package com.yunus.business.controller;

import com.yunus.business.dto.BulkWorkingHourRequest;
import com.yunus.business.dto.WorkingHourRequest;
import com.yunus.business.dto.WorkingHourResponse;
import com.yunus.business.service.WorkingHourService;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşletme ve personel çalışma saati yönetimi endpoint'leri.
 * Yetki ve sahip kontrolü service katmanında yapılır.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/working-hours")
public class WorkingHourController {

    private final WorkingHourService workingHourService;

    public WorkingHourController(WorkingHourService workingHourService) {
        this.workingHourService = workingHourService;
    }

    /**
     * İşletmenin genel çalışma saatlerini listeler.
     * GET /api/v1/businesses/{businessId}/working-hours
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<WorkingHourResponse>>> getBusinessHours(
            @PathVariable UUID businessId) {
        List<WorkingHourResponse> hours = workingHourService.getBusinessHours(businessId);
        return ResponseEntity.ok(BaseResponse.success(hours));
    }

    /**
     * Belirtilen personelin çalışma saatlerini listeler.
     * GET /api/v1/businesses/{businessId}/working-hours/staff/{staffId}
     */
    @GetMapping("/staff/{staffId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<WorkingHourResponse>>> getStaffHours(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId) {
        List<WorkingHourResponse> hours = workingHourService.getStaffHours(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success(hours));
    }

    /**
     * İşletme geneli çalışma saatlerini tamamen yeniden set eder.
     * Mevcut saatler silinir; yenileri oluşturulur.
     * PUT /api/v1/businesses/{businessId}/working-hours
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<WorkingHourResponse>>> setBusinessHours(
            @PathVariable UUID businessId,
            @Valid @RequestBody BulkWorkingHourRequest request) {
        List<WorkingHourResponse> hours = workingHourService.setBusinessHours(businessId, request);
        return ResponseEntity.ok(BaseResponse.success("Çalışma saatleri güncellendi", hours));
    }

    /**
     * Personelin çalışma saatlerini tamamen yeniden set eder.
     * PUT /api/v1/businesses/{businessId}/working-hours/staff/{staffId}
     */
    @PutMapping("/staff/{staffId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<WorkingHourResponse>>> setStaffHours(
            @PathVariable UUID businessId,
            @PathVariable UUID staffId,
            @Valid @RequestBody BulkWorkingHourRequest request) {
        List<WorkingHourResponse> hours = workingHourService.setStaffHours(businessId, staffId, request);
        return ResponseEntity.ok(BaseResponse.success("Personel çalışma saatleri güncellendi", hours));
    }

    /**
     * Tek bir günün çalışma saatini günceller.
     * PATCH /api/v1/businesses/{businessId}/working-hours/{workingHourId}
     */
    @PatchMapping("/{workingHourId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<WorkingHourResponse>> updateOneDay(
            @PathVariable UUID businessId,
            @PathVariable UUID workingHourId,
            @Valid @RequestBody WorkingHourRequest request) {
        WorkingHourResponse response = workingHourService.updateOneDay(businessId, workingHourId, request);
        return ResponseEntity.ok(BaseResponse.success("Çalışma saati güncellendi", response));
    }
}
