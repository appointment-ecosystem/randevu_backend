package com.yunus.localservice.location.repository;

import com.yunus.localservice.location.entity.Neighborhood;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Seçilen ilçeye göre aktif mahalle listesi.
 */
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, UUID> {

    List<Neighborhood> findByDistrictIdAndIsActiveTrue(UUID districtId);

}
