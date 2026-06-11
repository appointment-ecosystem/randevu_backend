package com.yunus.payment.config;

import com.iyzipay.Options;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * iyzico ödeme servisi için gerekli {@link Options} bean'ini oluşturan konfigürasyon sınıfı.
 */
@Configuration
@RequiredArgsConstructor
public class IyzicoConfig {

    private final IyzicoProperties iyzicoProperties;

    /**
     * iyzico SDK çağrılarında kullanılacak API anahtarı, gizli anahtar ve base URL bilgilerini
     * içeren {@link Options} nesnesini üretir.
     */
    @Bean
    public Options iyzicoOptions() {
        Options options = new Options();
        options.setApiKey(iyzicoProperties.getApiKey());
        options.setSecretKey(iyzicoProperties.getSecretKey());
        options.setBaseUrl(iyzicoProperties.getBaseUrl());
        return options;
    }

}
