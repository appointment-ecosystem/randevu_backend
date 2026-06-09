package com.yunus.notification.service;

import com.yunus.notification.dto.PushNotificationPayload;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    
    /**
     * Tek bir kullanıcının tüm aktif cihazlarına bildirim gönder (fire-and-forget)
     */
    void sendToUser(UUID userId, PushNotificationPayload payload);
    
    /**
     * Birden fazla kullanıcının aktif cihazlarına bildirim gönder (fire-and-forget)
     */
    void sendToUsers(List<UUID> userIds, PushNotificationPayload payload);
}
