package com.yunus.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * İşletme ile ilgili uygulama ayarları.
 */
@ConfigurationProperties(prefix = "app.business")
public record BusinessProperties(
        boolean autoApprove
) {
}

