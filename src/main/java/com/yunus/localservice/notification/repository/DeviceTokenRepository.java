package com.yunus.localservice.notification.repository;

import com.yunus.localservice.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * FCM cihaz token kayıtları; push bildirim gönderimi için (Faz 10).
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserIdAndIsActiveTrue(UUID userId);

}
