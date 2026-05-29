package com.yunus.sms.service;

import com.yunus.common.exception.BusinessException;
import com.yunus.sms.config.OtpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * OTP kodlarının üretilmesi, gönderilmesi ve doğrulanması işlemlerini yürüten Redis entegrasyonlu servis.
 * SecureRandom kullanarak 6 haneli kod üretir.
 * Hatalı deneme limitini (max-attempts) ve geçerlilik süresini (ttl-seconds) takip eder.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final String OTP_CODE_PREFIX = "otp:code:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp:attempts:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SmsService smsService;
    private final OtpProperties otpProperties;
    private final Environment environment;
    private final SecureRandom secureRandom;

    public OtpServiceImpl(RedisTemplate<String, String> redisTemplate,
                          SmsService smsService,
                          OtpProperties otpProperties,
                          Environment environment) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
        this.otpProperties = otpProperties;
        this.environment = environment;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public void sendOtp(String phone) {
        // 6 haneli rastgele OTP kodu üret (örn: 100000 - 999999)
        String code = String.valueOf(100000 + secureRandom.nextInt(900000));

        String codeKey = OTP_CODE_PREFIX + phone;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + phone;

        try {
            // Kodu Redis'e kaydet (TTL ile)
            redisTemplate.opsForValue().set(codeKey, code, otpProperties.ttlSeconds(), TimeUnit.SECONDS);
            // Deneme sayısını sıfırla
            redisTemplate.opsForValue().set(attemptsKey, "0", otpProperties.ttlSeconds(), TimeUnit.SECONDS);

            String message = String.format("Randevu platformu doğrulama kodunuz: %s. Bu kod %d dakika geçerlidir.",
                    code, otpProperties.ttlSeconds() / 60);

            smsService.sendSms(phone, message);
            log.info("OTP sent successfully to: {}", phone);
            if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                log.info("DEV ONLY — OTP for {}: {}", phone, code);
            }
        } catch (Exception ex) {
            log.error("Redis connection failed during OTP store for: {}", phone, ex);
            // Redis çökerse veya erişilemezse güvenli modda loga bas ve devam et (ya da istersen hata fırlat,
            // ama burada iş mantığı olarak Redis'in çalışıyor olması zorunludur)
            throw new BusinessException("Doğrulama kodu sistemi şu an hizmet veremiyor, lütfen daha sonra tekrar deneyiniz.");
        }
    }

    @Override
    public boolean verifyOtp(String phone, String code) {
        String codeKey = OTP_CODE_PREFIX + phone;
        String attemptsKey = OTP_ATTEMPTS_PREFIX + phone;

        try {
            String savedCode = redisTemplate.opsForValue().get(codeKey);
            if (savedCode == null) {
                log.warn("OTP verification failed: OTP expired or not found for phone: {}", phone);
                return false;
            }

            String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
            int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

            if (attempts >= otpProperties.maxAttempts()) {
                invalidateOtp(phone);
                log.warn("OTP verification blocked: Max attempts reached for phone: {}", phone);
                throw new BusinessException("Maksimum hatalı giriş limitine ulaştınız. Lütfen yeni bir kod isteyin.");
            }

            if (savedCode.equals(code)) {
                // Doğrulama başarılı ise kodları temizle
                invalidateOtp(phone);
                log.info("OTP verified successfully for phone: {}", phone);
                return true;
            } else {
                // Hatalı deneme sayısını arttır
                attempts++;
                redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts),
                        redisTemplate.getExpire(codeKey, TimeUnit.SECONDS), TimeUnit.SECONDS);
                log.warn("OTP verification failed: Incorrect code for phone: {}, attempts: {}", phone, attempts);
                return false;
            }

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Redis error during OTP verification for phone: {}", phone, ex);
            throw new BusinessException("Doğrulama işlemi sırasında sistemsel bir hata oluştu.");
        }
    }

    @Override
    public boolean hasActiveOtp(String phone) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(OTP_CODE_PREFIX + phone));
        } catch (Exception ex) {
            log.error("Redis error while checking OTP presence for phone: {}", phone, ex);
            throw new BusinessException("Doğrulama işlemi sırasında sistemsel bir hata oluştu.");
        }
    }

    @Override
    public void invalidateOtp(String phone) {
        try {
            redisTemplate.delete(OTP_CODE_PREFIX + phone);
            redisTemplate.delete(OTP_ATTEMPTS_PREFIX + phone);
            log.info("OTP keys invalidated for phone: {}", phone);
        } catch (Exception ex) {
            log.error("Failed to delete OTP keys in Redis for: {}", phone, ex);
        }
    }
}
