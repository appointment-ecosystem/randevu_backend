package com.yunus.notification.service.impl;

import com.yunus.notification.dto.DeviceTokenResponse;
import com.yunus.notification.dto.RegisterDeviceTokenRequest;
import com.yunus.notification.entity.DeviceToken;
import com.yunus.notification.repository.DeviceTokenRepository;
import com.yunus.notification.service.DeviceTokenService;
import com.yunus.user.entity.User;
import com.yunus.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Override
    public DeviceTokenResponse registerToken(UUID userId, RegisterDeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Optional<DeviceToken> existingTokenOpt = deviceTokenRepository.findByToken(request.getToken());

        DeviceToken deviceToken;
        if (existingTokenOpt.isPresent()) {
            deviceToken = existingTokenOpt.get();
            if (deviceToken.getUser().getId().equals(userId)) {
                deviceToken.setPlatform(request.getPlatform());
                deviceToken.setActive(true);
            } else {
                deviceToken.setActive(false);
                deviceTokenRepository.save(deviceToken);

                deviceToken = new DeviceToken();
                deviceToken.setUser(user);
                deviceToken.setToken(request.getToken());
                deviceToken.setPlatform(request.getPlatform());
                deviceToken.setActive(true);
            }
        } else {
            deviceToken = new DeviceToken();
            deviceToken.setUser(user);
            deviceToken.setToken(request.getToken());
            deviceToken.setPlatform(request.getPlatform());
            deviceToken.setActive(true);
        }

        deviceTokenRepository.save(deviceToken);

        return mapToResponse(deviceToken);
    }

    @Override
    public void deactivateToken(UUID userId, String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            if (deviceToken.getUser().getId().equals(userId)) {
                deviceToken.setActive(false);
                deviceTokenRepository.save(deviceToken);
            }
        });
    }

    @Override
    public List<DeviceTokenResponse> getUserTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DeviceTokenResponse mapToResponse(DeviceToken token) {
        return DeviceTokenResponse.builder()
                .id(token.getId())
                .token(maskToken(token.getToken()))
                .platform(token.getPlatform())
                .active(token.isActive())
                .createdAt(token.getCreatedAt())
                .build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 20) {
            return "***";
        }
        return token.substring(0, 20) + "***";
    }
}
