package com.yunus.notification.dto;

import com.yunus.notification.entity.DevicePlatform;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceTokenResponse {
    private UUID id;
    private String token;
    private DevicePlatform platform;
    private boolean active;
    private OffsetDateTime createdAt;
}
