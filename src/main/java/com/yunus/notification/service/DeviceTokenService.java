package com.yunus.notification.service;

import com.yunus.notification.dto.DeviceTokenResponse;
import com.yunus.notification.dto.RegisterDeviceTokenRequest;
import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {
    DeviceTokenResponse registerToken(UUID userId, RegisterDeviceTokenRequest request);
    void deactivateToken(UUID userId, String token);
    List<DeviceTokenResponse> getUserTokens(UUID userId);
}
