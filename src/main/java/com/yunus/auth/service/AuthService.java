package com.yunus.auth.service;

import com.yunus.auth.dto.AuthResponse;
import com.yunus.auth.dto.LoginRequest;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.common.exception.ConflictException;
import com.yunus.security.jwt.JwtService;
import com.yunus.security.service.CustomUserDetails;
import com.yunus.security.service.RefreshTokenService;
import com.yunus.user.entity.RefreshToken;
import com.yunus.user.entity.User;
import com.yunus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcı kayıt, giriş, logout ve token yenileme (refresh) işlerini yürüten servis.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Yeni bir kullanıcı kaydeder.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            log.warn("Register attempt failed: Phone number already exists: {}", request.getPhone());
            throw new ConflictException("Bu telefon numarası zaten kullanımda");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            log.warn("Register attempt failed: Email already exists: {}", request.getEmail());
            throw new ConflictException("Bu e-posta adresi zaten kullanımda");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setPhoneVerified(false);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId())
                .role(savedUser.getRole())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Kullanıcı girişi gerçekleştirir ve token çiftini döner.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            log.info("User logged in successfully: {}", user.getId());

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = refreshTokenService.createRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .role(user.getRole())
                    .tokenType("Bearer")
                    .build();
        } catch (BadCredentialsException ex) {
            log.warn("Login failed: invalid credentials for phone: {}", request.getPhone());
            throw ex; // GlobalExceptionHandler will catch and map to Turkish message
        }
    }

    /**
     * Refresh token ile access token yeniler (rotasyonlu refresh token).
     */
    @Transactional
    public AuthResponse refresh(String oldRefreshToken) {
        RefreshToken tokenEntity = refreshTokenService.validateRefreshToken(oldRefreshToken);
        User user = tokenEntity.getUser();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken);

        log.info("Token refreshed for user: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .role(user.getRole())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Oturumu kapatır ve refresh token'ı iptal eder.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    /**
     * Giriş yapmış kullanıcının profil detaylarını döner.
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .phoneVerified(user.getPhoneVerified())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .build();
    }
}
