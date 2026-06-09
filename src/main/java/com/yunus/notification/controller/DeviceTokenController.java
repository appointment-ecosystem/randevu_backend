package com.yunus.notification.controller;

import com.yunus.common.response.BaseResponse;
import com.yunus.notification.dto.DeviceTokenResponse;
import com.yunus.notification.dto.RegisterDeviceTokenRequest;
import com.yunus.notification.service.DeviceTokenService;
import com.yunus.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<DeviceTokenResponse>> registerToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        
        DeviceTokenResponse response = deviceTokenService.registerToken(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(BaseResponse.success("Cihaz başarıyla kaydedildi.", response));
    }

    @DeleteMapping("/{token}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<Void>> deactivateToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String token) {
        
        deviceTokenService.deactivateToken(userPrincipal.getUserId(), token);
        return ResponseEntity.ok(BaseResponse.success("Cihaz deaktive edildi."));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<List<DeviceTokenResponse>>> getUserTokens(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<DeviceTokenResponse> tokens = deviceTokenService.getUserTokens(userPrincipal.getUserId());
        return ResponseEntity.ok(BaseResponse.success("Kayıtlı cihazlar getirildi.", tokens));
    }
}
