package com.yunus.location.service;

import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.location.dto.CityResponse;
import com.yunus.location.dto.DistrictResponse;
import com.yunus.location.dto.NeighborhoodResponse;
import com.yunus.location.entity.City;
import com.yunus.location.entity.District;
import com.yunus.location.entity.Neighborhood;
import com.yunus.location.repository.CityRepository;
import com.yunus.location.repository.DistrictRepository;
import com.yunus.location.repository.NeighborhoodRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Konum verilerini okur ve entity → DTO dönüşümü yapar.
 */
@Service
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;

    public LocationServiceImpl(CityRepository cityRepository,
                               DistrictRepository districtRepository,
                               NeighborhoodRepository neighborhoodRepository) {
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
    }

    @Override
    public List<CityResponse> getCities() {
        return cityRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toCityResponse)
                .toList();
    }

    @Override
    public List<DistrictResponse> getDistrictsByCity(UUID cityId) {
        City city = cityRepository.findById(cityId)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Şehir", "id", cityId));

        return districtRepository.findByCityIdAndIsActiveTrue(city.getId()).stream()
                .map(district -> toDistrictResponse(district, city.getId()))
                .toList();
    }

    @Override
    public List<NeighborhoodResponse> getNeighborhoodsByDistrict(UUID districtId) {
        District district = districtRepository.findById(districtId)
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("İlçe", "id", districtId));

        return neighborhoodRepository.findByDistrictIdAndIsActiveTrue(district.getId()).stream()
                .map(neighborhood -> toNeighborhoodResponse(neighborhood, district.getId()))
                .toList();
    }

    private CityResponse toCityResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getCode(),
                city.getIsActive(),
                city.getCreatedAt(),
                city.getUpdatedAt()
        );
    }

    private DistrictResponse toDistrictResponse(District district, UUID cityId) {
        return new DistrictResponse(
                district.getId(),
                cityId,
                district.getName(),
                district.getIsActive(),
                district.getCreatedAt(),
                district.getUpdatedAt()
        );
    }

    private NeighborhoodResponse toNeighborhoodResponse(Neighborhood neighborhood, UUID districtId) {
        return new NeighborhoodResponse(
                neighborhood.getId(),
                districtId,
                neighborhood.getName(),
                neighborhood.getIsActive(),
                neighborhood.getCreatedAt(),
                neighborhood.getUpdatedAt()
        );
    }
}
