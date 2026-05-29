package com.yunus.business.controller;

import com.yunus.business.dto.HolidayRequest;
import com.yunus.business.dto.HolidayResponse;
import com.yunus.business.service.HolidayService;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tatil / kapalı gün yönetimi endpoint'leri.
 * Yetki kontrolü service katmanında yapılır; controller yalnızca yönlendirme sağlar.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * Tatilleri listeler.
     * staffId query param verilmezse işletme geneli tatiller; verilirse personele özel tatiller döner.
     * GET /api/v1/businesses/{businessId}/holidays
     * GET /api/v1/businesses/{businessId}/holidays?staffId={staffId}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<HolidayResponse>>> getHolidays(
            @PathVariable UUID businessId,
            @RequestParam(required = false) UUID staffId) {
        List<HolidayResponse> holidays = holidayService.getHolidays(businessId, staffId);
        return ResponseEntity.ok(BaseResponse.success(holidays));
    }

    /**
     * Yeni tatil ekler; aynı gün aynı kapsam için tekrar eklenemez.
     * POST /api/v1/businesses/{businessId}/holidays
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<HolidayResponse>> addHoliday(
            @PathVariable UUID businessId,
            @Valid @RequestBody HolidayRequest request) {
        HolidayResponse response = holidayService.addHoliday(businessId, request);
        return ResponseEntity.ok(BaseResponse.success("Tatil başarıyla eklendi", response));
    }

    /**
     * Tatil kaydını siler; geri alınamaz.
     * DELETE /api/v1/businesses/{businessId}/holidays/{holidayId}
     */
    @DeleteMapping("/{holidayId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deleteHoliday(
            @PathVariable UUID businessId,
            @PathVariable UUID holidayId) {
        holidayService.deleteHoliday(businessId, holidayId);
        return ResponseEntity.ok(BaseResponse.success("Tatil silindi"));
    }
}
