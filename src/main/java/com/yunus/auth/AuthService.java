package com.yunus.auth;

import com.yunus.auth.dto.AuthResponse;
import com.yunus.auth.dto.LoginRequest;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.common.exception.ConflictException;
import com.yunus.security.JwtService;
import com.yunus.security.UserPrincipal;
import com.yunus.user.entity.RefreshToken;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcı kayıt, giriş, logout ve token yenileme (refresh) işlerini yürüten servis.
 * Refresh token yaşam döngüsü RefreshTokenService tarafından yönetilir.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager,
                       RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.redisTemplate = redisTemplate;
    }

    // ─── Auth Operations ──────────────────────────────────────────────────────

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
        // Güvenlik: public register endpoint'i üzerinden her zaman USER rolü verilir.
        // BUSINESS_OWNER, BUSINESS_EMPLOYEE ve ADMIN rolleri ayrı admin flow'ları üzerinden atanır.
        user.setRole(UserRole.USER);
        user.setPhoneVerified(false);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        UserPrincipal userPrincipal = new UserPrincipal(savedUser);
        String accessToken = jwtService.generateAccessToken(userPrincipal);
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

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();

            log.info("User logged in successfully: {}", user.getId());

            String accessToken = jwtService.generateAccessToken(userPrincipal);
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
            throw ex; // GlobalExceptionHandler yakalar ve Türkçe mesaj döner
        }
    }

    /**
     * Refresh token ile access token yeniler (rotasyonlu refresh token).
     */
    @Transactional
    public AuthResponse refresh(String oldRefreshToken) {
        RefreshToken tokenEntity = refreshTokenService.validateRefreshToken(oldRefreshToken);
        User user = tokenEntity.getUser();

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);
        String newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshToken, tokenEntity);

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
     * Oturumu kapatır: refresh token DB'de revoke edilir, access token Redis blacklist'e eklenir.
     *
     * @param rawRefreshToken İptal edilecek ham refresh token
     * @param accessToken     Blacklist'e eklenecek access token; null ise bu adım atlanır
     */
    @Transactional
    public void logout(String rawRefreshToken, String accessToken) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);

        if (accessToken != null) {
            try {
                long remainingMs = jwtService.getRemainingExpiration(accessToken);
                if (remainingMs > 0) {
                    redisTemplate.opsForValue().set(
                            BLACKLIST_PREFIX + accessToken,
                            "true",
                            remainingMs,
                            TimeUnit.MILLISECONDS
                    );
                    log.info("Access token blacklisted in Redis, TTL: {} ms", remainingMs);
                } else {
                    log.debug("Access token already expired, skipping blacklist.");
                }
            } catch (Exception ex) {
                // Token parse hatası veya Redis erişim hatası — logout akışını durdurma
                log.warn("Access token could not be blacklisted during logout: {}", ex.getMessage());
            }
        }
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
