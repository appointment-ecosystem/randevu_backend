package com.yunus.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS konfigürasyonunu application.yml / environment variable'lardan okur.
 * Dev: localhost originleri yeterli.
 * Prod: application-prod.yml veya env variable üzerinden gerçek domain'ler verilir.
 * allowCredentials + wildcard origin birlikte KULLANILMAZ — tarayıcı engeller.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
