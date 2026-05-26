package com.yunus.location.repository;

import com.yunus.location.entity.District;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Seçilen ile göre aktif ilçe listesi.
 */
public interface DistrictRepository extends JpaRepository<District, UUID> {

    List<District> findByCityIdAndIsActiveTrue(UUID cityId);

}
