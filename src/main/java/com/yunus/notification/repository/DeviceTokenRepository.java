package com.yunus.notification.repository;

import com.yunus.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    @Query("SELECT dt.token FROM DeviceToken dt WHERE dt.user.id = :userId AND dt.active = true")
    List<String> findActiveTokensByUserId(@Param("userId") UUID userId);

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    @Query("SELECT dt.token FROM DeviceToken dt WHERE dt.user.id IN :userIds AND dt.active = true")
    List<String> findActiveTokensByUserIds(@Param("userIds") List<UUID> userIds);
}
