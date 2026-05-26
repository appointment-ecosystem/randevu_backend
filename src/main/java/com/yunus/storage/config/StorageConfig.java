package com.yunus.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;

/**
 * Cloudflare R2 / S3 uyumlu nesne depolama (Object Storage) bağlantı yapılandırması.
 * software.amazon.awssdk:s3 kütüphanesini kullanır.
 */
@Configuration
public class StorageConfig {

    private final StorageProperties storageProperties;

    public StorageConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageProperties.accessKey(),
                storageProperties.secretKey()
        );

        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                // R2 API'si için bölge (region) "auto" veya herhangi bir değer olmalıdır, genelde us-east-1 verilir.
                .region(Region.US_EAST_1)
                // Path style access, endpoint tabanlı R2 / custom S3 servisleri için gereklidir.
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
