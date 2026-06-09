package com.yunus.notification.dto;

import com.yunus.notification.entity.NotificationType;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PushNotificationPayload {
    private String title;
    private String body;
    private NotificationType type;
    private Map<String, String> data;
}
