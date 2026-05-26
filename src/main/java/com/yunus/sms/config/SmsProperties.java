package com.yunus.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Netgsm SMS API bağlantı ve kimlik doğrulama ayarlarını okur.
 */
@ConfigurationProperties(prefix = "app.sms.netgsm")
public record SmsProperties(
        String apiUrl,
        String username,
        String password,
        String header
) {
}
