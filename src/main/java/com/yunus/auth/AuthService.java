package com.yunus.auth;

import com.yunus.auth.dto.AuthResponse;
import com.yunus.auth.dto.LoginRequest;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.common.exception.ConflictException;
import com.yunus.common.exception.UnauthorizedException;
import com.yunus.security.JwtService;
import com.yunus.security.UserPrincipal;
import com.yunus.user.entity.RefreshToken;
import com.yunus.user.entity.User;
import com.yunus.user.repository.RefreshTokenRepository;
import com.yunus.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
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
 * RefreshToken yönetimi de bu sınıf içinde konumlandırılmıştır.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Access token süresi: 15 dakika (ms), Refresh token süresi: 7 gün (ms) — JwtProperties'ten okunur
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final com.yunus.security.jwt.JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       com.yunus.security.jwt.JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.jwtProperties = jwtProperties;
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
        user.setRole(request.getRole());
        user.setPhoneVerified(false);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        UserPrincipal userPrincipal = new UserPrincipal(savedUser);
        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = createRefreshToken(savedUser);

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
            String refreshToken = createRefreshToken(user);

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
        RefreshToken tokenEntity = validateRefreshToken(oldRefreshToken);
        User user = tokenEntity.getUser();

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = jwtService.generateAccessToken(userPrincipal);
        String newRefreshToken = rotateRefreshToken(oldRefreshToken, tokenEntity);

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
    public void logout(String rawRefreshToken) {
        revokeRefreshToken(rawRefreshToken);
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

    // ─── Refresh Token Operations ─────────────────────────────────────────────

    /**
     * Kullanıcı için yeni bir refresh token üretir ve hash'ini veri tabanına kaydeder.
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(OffsetDateTime.now().plusNanos(jwtProperties.refreshTokenExpiration() * 1_000_000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        log.info("New refresh token created for user: {}", user.getId());
        return rawToken;
    }

    /**
     * Ham refresh token değerini doğrular.
     * Geçerli ise ilişkili RefreshToken nesnesini döner.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Geçersiz oturum anahtarı"));

        if (token.getRevoked()) {
            log.warn("Revoked refresh token attempt for user: {}", token.getUser().getId());
            throw new UnauthorizedException("Bu oturum sonlandırılmış");
        }

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Expired refresh token attempt for user: {}", token.getUser().getId());
            throw new UnauthorizedException("Oturum süresi dolmuş, lütfen tekrar giriş yapın");
        }

        return token;
    }

    /**
     * Ham refresh token'ı iptal eder (Logout durumunda).
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user: {}", token.getUser().getId());
        });
    }

    /**
     * Token rotasyonu gerçekleştirir: Eski token'ı iptal eder, yeni bir token üretip döner.
     */
    @Transactional
    public String rotateRefreshToken(String oldRawToken, RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        log.info("Old refresh token revoked, rotating token for user: {}", oldToken.getUser().getId());
        return createRefreshToken(oldToken.getUser());
    }

    /**
     * SHA-256 ile ham token değerini hash'ler.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new IllegalStateException("Hata: SHA-256 bulunamadı");
        }
    }
}
