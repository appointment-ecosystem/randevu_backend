package com.yunus.appointment.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Randevu slot'larına yönelik dağıtık Redis kilitleri yönetir.
 *
 * <p>Amaç: Aynı slot için eşzamanlı gelen birden fazla randevu isteğinin
 * çakışmasını önlemek. Kilit, yalnızca belirli bir kullanıcıya verilir ve
 * 5 dakika sonra otomatik olarak sona erer.
 *
 * <p>Key şeması (mevcut key şemalarıyla çakışmaz):
 * <pre>
 *   slot:lock:{businessId}:{staffId}:{startTime}
 *   — staffId null ise "none" kullanılır
 * </pre>
 *
 * <p>Mevcut diğer key şemaları (referans):
 * <ul>
 *   <li>{@code otp:{phone}} — OTP doğrulama</li>
 *   <li>{@code blacklist:{token}} — JWT kara liste</li>
 * </ul>
 *
 * <p>Mimari not: Bu servis tek başına yeterli değildir. Tam koruma için
 * DB katmanındaki unique constraint {@code (staff_id, start_time)} ile
 * birlikte kullanılmalıdır.
 */
@Service
public class SlotLockService {

    /** Slot kilidinin varsayılan yaşam süresi. */
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    /** Key öneki — diğer Redis anahtarlarıyla çakışmayı önler. */
    private static final String KEY_PREFIX = "slot:lock:";

    private final RedisTemplate<String, String> redisTemplate;

    public SlotLockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Belirtilen slot için Redis'te geçici bir kilit almayı dener.
     *
     * <p>Redis {@code SETNX} semantiği kullanılır: anahtar zaten mevcutsa
     * {@code false} döner ve kilide dokunulmaz. Başarılıysa 5 dakikalık
     * TTL ile birlikte atomik olarak set edilir.
     *
     * <p>Kullanım: {@code AppointmentService.createAppointment()} içinde
     * DB yazımından önce çağrılmalıdır.
     *
     * @param businessId işletmenin UUID'si
     * @param staffId    personelin UUID'si; personelsiz işletmelerde {@code null}
     * @param startTime  slotun başlangıç zamanı
     * @param userId     kilidi almaya çalışan kullanıcının UUID'si
     * @return {@code true} kilit başarıyla alındıysa, {@code false} slot zaten kilitliyse
     */
    public boolean tryLock(UUID businessId, UUID staffId, OffsetDateTime startTime, UUID userId) {
        String key = buildKey(businessId, staffId, startTime);
        String value = userId.toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, value, LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Daha önce alınan slot kilidini serbest bırakır.
     *
     * <p>Güvenlik kuralı: yalnızca kilidi orijinal olarak alan kullanıcı
     * ({@code userId}) kilidi silebilir. Başka bir kullanıcıya ait kilide
     * dokunulmaz; bu durum sessizce görmezden gelinir.
     *
     * <p>Kullanım:
     * <ul>
     *   <li>Randevu başarıyla oluşturulduktan sonra (DB kaydedildikten sonra)</li>
     *   <li>İstek hata aldığında {@code finally} bloğunda</li>
     * </ul>
     *
     * @param businessId işletmenin UUID'si
     * @param staffId    personelin UUID'si; personelsiz işletmelerde {@code null}
     * @param startTime  slotun başlangıç zamanı
     * @param userId     kilidi bırakmak isteyen kullanıcının UUID'si
     */
    public void releaseLock(UUID businessId, UUID staffId, OffsetDateTime startTime, UUID userId) {
        String key = buildKey(businessId, staffId, startTime);
        String currentValue = redisTemplate.opsForValue().get(key);
        if (Objects.equals(currentValue, userId.toString())) {
            redisTemplate.delete(key);
        }
    }

    /**
     * Belirtilen slot'un şu an kilitli olup olmadığını kontrol eder.
     *
     * <p>Kullanım: Slot listesi dönerken her bir slot için müsaitlik
     * durumunu ({@link com.yunus.appointment.dto.SlotResponse#available()})
     * hesaplamada yardımcı kontrol olarak kullanılabilir.
     *
     * @param businessId işletmenin UUID'si
     * @param staffId    personelin UUID'si; personelsiz işletmelerde {@code null}
     * @param startTime  slotun başlangıç zamanı
     * @return {@code true} slot kilitliyse, {@code false} müsaitse
     */
    public boolean isLocked(UUID businessId, UUID staffId, OffsetDateTime startTime) {
        String key = buildKey(businessId, staffId, startTime);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Redis anahtar dizisini oluşturur.
     *
     * <p>Format: {@code slot:lock:{businessId}:{staffId}:{startTime}}
     * <br>{@code staffId} null olduğunda "none" kullanılır.
     *
     * @param businessId işletmenin UUID'si
     * @param staffId    personelin UUID'si veya {@code null}
     * @param startTime  slotun başlangıç zamanı
     * @return oluşturulan Redis anahtarı
     */
    private String buildKey(UUID businessId, UUID staffId, OffsetDateTime startTime) {
        String staffSegment = (staffId != null) ? staffId.toString() : "none";
        return KEY_PREFIX + businessId + ":" + staffSegment + ":" + startTime;
    }
}
