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
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;

    static {
        RELEASE_LOCK_SCRIPT = new org.springframework.data.redis.core.script.DefaultRedisScript<>();
        RELEASE_LOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end"
        );
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * Belirtilen slot için rastgele UUID token kullanarak kilit almayı dener.
     *
     * @param businessId İşletme kimliği
     * @param staffId    Personel kimliği (nullable)
     * @param startTime  Slot başlangıç saati
     * @param lockToken  Kilit sahibini temsil eden rastgele UUID token
     * @return Kilit başarıyla alındıysa true
     */
    public boolean tryAcquire(UUID businessId, UUID staffId, OffsetDateTime startTime, String lockToken) {
        String key = buildKey(businessId, staffId, startTime);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, lockToken, LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Lua compare-and-delete scripti ile kilidi atomik olarak serbest bırakır.
     *
     * @param businessId İşletme kimliği
     * @param staffId    Personel kimliği (nullable)
     * @param startTime  Slot başlangıç saati
     * @param lockToken  Kilidin sahibi olduğunu kanıtlayan token
     * @return Kilit başarıyla silindiyse true
     */
    public boolean release(UUID businessId, UUID staffId, OffsetDateTime startTime, String lockToken) {
        String key = buildKey(businessId, staffId, startTime);
        Long result = redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                java.util.Collections.singletonList(key),
                lockToken
        );
        return result != null && result > 0;
    }

    /**
     * Rastgele UUID token oluşturarak kilit dener (istek başına benzersiz token).
     */
    public boolean tryLock(UUID businessId, UUID staffId, OffsetDateTime startTime, UUID userId) {
        String token = UUID.randomUUID().toString();
        return tryAcquire(businessId, staffId, startTime, token);
    }

    /**
     * Lua compare-and-delete kullanarak kilidi atomik olarak serbest bırakır (geriye dönük uyumluluk).
     */
    public void releaseLock(UUID businessId, UUID staffId, OffsetDateTime startTime, UUID userId) {
        if (userId != null) {
            release(businessId, staffId, startTime, userId.toString());
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
