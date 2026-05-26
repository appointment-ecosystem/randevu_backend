package com.yunus.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Storage (Cloudflare R2 / S3 uyumlu) yapılandırma değerlerini okur.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String bucketName,
        String accessKey,
        String secretKey,
        String publicUrl
) {
}
