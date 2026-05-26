package com.yunus.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT yapılandırma değerlerini application config'ten okur.
 * secret, access token ve refresh token süreleri burada tanımlanır.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {
}
