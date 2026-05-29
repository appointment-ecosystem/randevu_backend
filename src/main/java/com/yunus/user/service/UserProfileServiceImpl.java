package com.yunus.user.service;

import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ConflictException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.sms.service.OtpService;
import com.yunus.user.dto.ChangePasswordRequest;
import com.yunus.user.dto.PhoneVerifyRequest;
import com.yunus.user.dto.UpdateProfileRequest;
import com.yunus.user.dto.UserProfileResponse;
import com.yunus.user.entity.RefreshToken;
import com.yunus.user.entity.User;
import com.yunus.user.repository.RefreshTokenRepository;
import com.yunus.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcının kendi profil işlemlerini yönetir.
 */
@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public UserProfileServiceImpl(UserRepository userRepository,
                                  RefreshTokenRepository refreshTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  OtpService otpService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        User user = getActiveUser(userId);
        return toUserProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = request.email().trim();
            userRepository.findByEmail(normalizedEmail)
                    .filter(found -> !found.getId().equals(userId))
                    .ifPresent(found -> {
                        throw new ConflictException("Bu e-posta adresi zaten kullanımda");
                    });
            user.setEmail(normalizedEmail);
        }

        User savedUser = userRepository.save(user);
        return toUserProfileResponse(savedUser);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getActiveUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mevcut şifre hatalı");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void requestPhoneOtp(UUID userId) {
        User user = getActiveUser(userId);

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new BusinessException("Telefon numarası zaten doğrulandı");
        }

        otpService.sendOtp(user.getPhone());
        log.info("Phone OTP requested for user: {}", userId);
    }

    @Override
    public void verifyPhone(UUID userId, PhoneVerifyRequest request) {
        User user = getActiveUser(userId);

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            throw new BusinessException("Telefon numarası zaten doğrulandı");
        }

        if (!otpService.hasActiveOtp(user.getPhone())) {
            throw new BusinessException("OTP süresi dolmuş veya bulunamadı");
        }

        boolean verified = otpService.verifyOtp(user.getPhone(), request.otpCode());
        if (!verified) {
            throw new BusinessException("OTP kodu geçersiz");
        }

        user.setPhoneVerified(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateMyAccount(UUID userId) {
        User user = getActiveUser(userId);
        user.setIsActive(false);
        userRepository.save(user);

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);

        log.info("User deactivated and refresh tokens revoked: {}", userId);
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResourceNotFoundException("Kullanıcı", "id", userId);
        }
        return user;
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getPhoneVerified(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
