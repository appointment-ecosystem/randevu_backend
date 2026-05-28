package com.yunus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Güvenlik davranışını kontrol eden konfigürasyon özellikleri.
 * failOnRedisBlacklistError:
 *   false (dev) → Redis erişilemezse blacklist kontrolü atlanır, sistem ayakta kalır.
 *   true  (prod) → Redis erişilemezse token geçersiz sayılır, daha güvenli ama daha kırılgan.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean failOnRedisBlacklistError
) {
}
