package com.yunus.location.controller;

import com.yunus.common.response.BaseResponse;
import com.yunus.location.dto.CityResponse;
import com.yunus.location.dto.DistrictResponse;
import com.yunus.location.dto.NeighborhoodResponse;
import com.yunus.location.service.LocationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İl, ilçe ve mahalle listeleme API'leri. Kimlik doğrulama gerektirmez.
 */
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Aktif illeri listeler.
     * GET /api/v1/locations/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<BaseResponse<List<CityResponse>>> getCities() {
        List<CityResponse> cities = locationService.getCities();
        return ResponseEntity.ok(BaseResponse.success(cities));
    }

    /**
     * Seçilen ile ait aktif ilçeleri listeler.
     * GET /api/v1/locations/cities/{cityId}/districts
     */
    @GetMapping("/cities/{cityId}/districts")
    public ResponseEntity<BaseResponse<List<DistrictResponse>>> getDistrictsByCity(
            @PathVariable UUID cityId) {
        List<DistrictResponse> districts = locationService.getDistrictsByCity(cityId);
        return ResponseEntity.ok(BaseResponse.success(districts));
    }

    /**
     * Seçilen ilçeye ait aktif mahalleleri listeler.
     * GET /api/v1/locations/districts/{districtId}/neighborhoods
     */
    @GetMapping("/districts/{districtId}/neighborhoods")
    public ResponseEntity<BaseResponse<List<NeighborhoodResponse>>> getNeighborhoodsByDistrict(
            @PathVariable UUID districtId) {
        List<NeighborhoodResponse> neighborhoods = locationService.getNeighborhoodsByDistrict(districtId);
        return ResponseEntity.ok(BaseResponse.success(neighborhoods));
    }
}
