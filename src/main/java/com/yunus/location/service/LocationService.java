package com.yunus.location.service;

import com.yunus.location.dto.CityResponse;
import com.yunus.location.dto.DistrictResponse;
import com.yunus.location.dto.NeighborhoodResponse;
import java.util.List;
import java.util.UUID;

/**
 * Konum (il, ilçe, mahalle) sorgulama işlemleri.
 */
public interface LocationService {

    List<CityResponse> getCities();

    List<DistrictResponse> getDistrictsByCity(UUID cityId);

    List<NeighborhoodResponse> getNeighborhoodsByDistrict(UUID districtId);
}
