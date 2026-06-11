package com.yunus.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml içindeki "iyzico" prefix'ine sahip ayarları taşıyan konfigürasyon sınıfı.
 */
@Data
@Component
@ConfigurationProperties(prefix = "iyzico")
public class IyzicoProperties {

    // iyzico tarafından sağlanan API anahtarı
    private String apiKey;

    // iyzico tarafından sağlanan gizli anahtar
    private String secretKey;

    // iyzico API'sinin temel adresi (sandbox veya production)
    private String baseUrl;

    // 3D Secure ödeme sonrası iyzico'nun yönlendireceği geri çağırma adresi
    private String callbackUrl;

    // Kapora tutarının toplam tutara oranı (örn. 0.20 -> %20)
    private double depositRate = 0.20;

}
