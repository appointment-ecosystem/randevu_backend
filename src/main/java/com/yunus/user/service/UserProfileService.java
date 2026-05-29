package com.yunus.user.service;

import com.yunus.user.dto.ChangePasswordRequest;
import com.yunus.user.dto.PhoneVerifyRequest;
import com.yunus.user.dto.UpdateProfileRequest;
import com.yunus.user.dto.UserProfileResponse;
import java.util.UUID;

/**
 * Kullanıcının kendi profil işlemleri.
 */
public interface UserProfileService {

    UserProfileResponse getMyProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    void requestPhoneOtp(UUID userId);

    void verifyPhone(UUID userId, PhoneVerifyRequest request);

    void deactivateMyAccount(UUID userId);
}
