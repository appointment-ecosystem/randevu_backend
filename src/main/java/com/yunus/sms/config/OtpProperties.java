package com.yunus.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OTP (Tek Kullanımlık Şifre) doğrulama ayarlarını okur.
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(
        int ttlSeconds,
        int maxAttempts
) {
}
