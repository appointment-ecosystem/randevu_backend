package com.yunus.auth;

import com.yunus.common.exception.UnauthorizedException;
import com.yunus.security.jwt.JwtProperties;
import com.yunus.user.entity.RefreshToken;
import com.yunus.user.entity.User;
import com.yunus.user.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh token üretimi, doğrulanması, iptal edilmesi ve rotasyon işlemlerini yürüten servis.
 * Güvenlik nedeniyle ham token veri tabanında saklanmaz, SHA-256 hash değeri saklanır.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

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
