package com.yunus.notification.service.impl;

import com.yunus.notification.dto.PushNotificationPayload;
import com.yunus.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "false")
public class NotificationFallbackService implements NotificationService {

    @Override
    public void sendToUser(UUID userId, PushNotificationPayload payload) {
        log.info("FCM devre dışı - bildirim gönderilmedi: user={}, title={}", userId, payload.getTitle());
    }

    @Override
    public void sendToUsers(List<UUID> userIds, PushNotificationPayload payload) {
        log.info("FCM devre dışı - bildirim gönderilmedi: usersCount={}, title={}", userIds.size(), payload.getTitle());
    }
}
