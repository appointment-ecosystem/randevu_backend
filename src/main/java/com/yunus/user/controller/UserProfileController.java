package com.yunus.user.controller;

import com.yunus.common.response.BaseResponse;
import com.yunus.security.CurrentUserService;
import com.yunus.user.dto.ChangePasswordRequest;
import com.yunus.user.dto.PhoneOtpRequest;
import com.yunus.user.dto.PhoneVerifyRequest;
import com.yunus.user.dto.UpdateProfileRequest;
import com.yunus.user.dto.UserProfileResponse;
import com.yunus.user.service.UserProfileService;
import com.yunus.ratelimit.annotation.KeyType;
import com.yunus.ratelimit.annotation.RateLimit;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş yapan kullanıcının kendi profil işlemleri.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@PreAuthorize("hasAnyRole('USER','BUSINESS_OWNER','ADMIN')")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserService currentUserService;

    public UserProfileController(UserProfileService userProfileService, CurrentUserService currentUserService) {
        this.userProfileService = userProfileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<UserProfileResponse>> getMyProfile() {
        UUID userId = currentUserService.getCurrentUserId();
        UserProfileResponse response = userProfileService.getMyProfile(userId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PatchMapping
    public ResponseEntity<BaseResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        UserProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(BaseResponse.success("Profil başarıyla güncellendi", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<BaseResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        userProfileService.changePassword(userId, request);
        return ResponseEntity.ok(BaseResponse.success("Şifre başarıyla güncellendi"));
    }

    // 10 dakika penceresi içinde aynı IP'den en fazla 3 OTP isteği
    @RateLimit(limit = 3, windowSeconds = 600, key = "user:otp", keyType = KeyType.IP)
    @PostMapping("/phone/otp")
    public ResponseEntity<BaseResponse<Void>> requestPhoneOtp(@RequestBody(required = false) PhoneOtpRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        userProfileService.requestPhoneOtp(userId);
        return ResponseEntity.ok(BaseResponse.success("Doğrulama kodu gönderildi"));
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<BaseResponse<Void>> verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        userProfileService.verifyPhone(userId, request);
        return ResponseEntity.ok(BaseResponse.success("Telefon numarası doğrulandı"));
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> deactivateMyAccount() {
        UUID userId = currentUserService.getCurrentUserId();
        userProfileService.deactivateMyAccount(userId);
        return ResponseEntity.ok(BaseResponse.success("Hesap pasifleştirildi"));
    }
}
