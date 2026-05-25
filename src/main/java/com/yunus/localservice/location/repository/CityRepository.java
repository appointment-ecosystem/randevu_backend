package com.yunus.localservice.location.repository;

import com.yunus.localservice.location.entity.City;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * İl listesi; seed data ve admin konum yönetimi için.
 */
public interface CityRepository extends JpaRepository<City, UUID> {

}
