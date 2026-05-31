package com.yunus.ratelimit.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yunus.exception.ErrorResponse;
import com.yunus.exception.ErrorType;
import com.yunus.ratelimit.annotation.KeyType;
import com.yunus.ratelimit.annotation.RateLimit;
import com.yunus.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sliding window algoritması ile Redis tabanlı rate limiting uygulayan interceptor.
 * Spring MVC'nin preHandle aşamasında çalışır; @RateLimit annotation'ı olan metodlara
 * istek sayısı denetimi uygular.
 *
 * <p>Algoritma — Sorted Set (ZSET) üzerinde sliding window:
 * <ol>
 *   <li>Şu anki zaman damgasını (epoch ms) al.</li>
 *   <li>Pencere başlangıcı = now - windowMs.</li>
 *   <li>Pencere dışındaki kayıtları ZREMRANGEBYSCORE ile temizle.</li>
 *   <li>ZCARD ile mevcut istek sayısını al.</li>
 *   <li>Sayı &gt;= limit ise HTTP 429 dön.</li>
 *   <li>Yeni isteği ZADD ile ekle (score = member = now).</li>
 *   <li>EXPIRE ile TTL'i windowSeconds'a ayarla.</li>
 * </ol>
 *
 * <p>Key formatı: {@code rate_limit:{annotation.key()}:{identifier}}
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** Redis key namespace; tüm rate limit anahtarları bu prefix ile başlar. */
    private static final String KEY_PREFIX = "rate_limit:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    /**
     * Bağımlılıkları constructor üzerinden enjekte eden yapıcı metot.
     *
     * @param redisTemplate Redis işlemleri için template
     * @param jwtService    JWT token çözümleme servisi
     */
    public RateLimitInterceptor(RedisTemplate<String, String> redisTemplate,
                                JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
        // OffsetDateTime serializasyonu için JavaTimeModule kaydet
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Her istek geldiğinde çalışır.
     * Handler metodu @RateLimit annotation taşımıyorsa hiçbir şey yapmadan true döner.
     * Limit aşılmışsa HTTP 429 yanıtı yazar ve false döner.
     *
     * @param request  Gelen HTTP isteği
     * @param response HTTP yanıtı
     * @param handler  İşlenecek controller metodu veya başka bir handler
     * @return Limit aşılmamışsa true; aşılmışsa false
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        // Yalnızca HandlerMethod (controller metodu) üzerinde annotation ara
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // @RateLimit annotation'ı var mı kontrol et — yoksa geç
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        // Identifier belirle: IP veya USER
        String identifier = resolveIdentifier(request, rateLimit.keyType());

        // Redis key oluştur: rate_limit:{endpointKey}:{identifier}
        String redisKey = KEY_PREFIX + rateLimit.key() + ":" + identifier;

        long windowMs = (long) rateLimit.windowSeconds() * 1000L;
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // Sliding window: eski kayıtları temizle ve mevcut sayıyı al
        ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();

        // Pencere dışındaki kayıtları sil (score < windowStart)
        zSet.removeRangeByScore(redisKey, 0, windowStart);

        // Mevcut penceredeki istek sayısını al
        Long requestCount = zSet.zCard(redisKey);
        long count = (requestCount != null) ? requestCount : 0L;

        log.debug("Rate limit check — key: {}, count: {}/{}", redisKey, count, rateLimit.limit());

        // Limit aşıldı mı?
        if (count >= rateLimit.limit()) {
            log.warn("Rate limit exceeded — key: {}, count: {}, limit: {}", redisKey, count, rateLimit.limit());
            writeRateLimitResponse(response);
            return false;
        }

        // Yeni isteği ZSET'e ekle (score = member = epoch ms; üst üste gelen ms için member benzersiz olsun)
        String member = now + ":" + Thread.currentThread().getId();
        zSet.add(redisKey, member, now);

        // TTL güncelle
        redisTemplate.expire(redisKey, rateLimit.windowSeconds(),
                java.util.concurrent.TimeUnit.SECONDS);

        return true;
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    /**
     * KeyType'a göre istek identifier'ını belirler.
     * KeyType.USER ise Authorization header'dan Bearer token alır ve userId çeker.
     * Token alınamazsa veya parse edilemezse IP'ye düşer.
     *
     * @param request  Gelen HTTP isteği
     * @param keyType  Annotation'dan gelen anahtar türü
     * @return IP adresi veya userId string
     */
    private String resolveIdentifier(HttpServletRequest request, KeyType keyType) {
        if (keyType == KeyType.USER) {
            String userId = extractUserIdFromRequest(request);
            if (userId != null && !userId.isBlank()) {
                return "user:" + userId;
            }
            // Token alınamadıysa IP'ye düş
            log.debug("Rate limit USER mode: token alınamadı, IP'ye düşülüyor");
        }
        return "ip:" + extractClientIp(request);
    }

    /**
     * Authorization header'dan Bearer token alıp JwtService üzerinden userId çeker.
     * Header yoksa, token formatı yanlışsa veya JWT parse hatası oluşursa null döner.
     *
     * @param request Gelen HTTP isteği
     * @return userId string veya null
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtService.extractUserIdSafe(token);
    }

    /**
     * İstemcinin gerçek IP adresini döner.
     * Önce X-Forwarded-For başlığına bakar (proxy/load balancer senaryoları için);
     * yoksa HttpServletRequest.getRemoteAddr() kullanır.
     *
     * @param request Gelen HTTP isteği
     * @return Tespit edilen IP adresi
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Birden fazla proxy varsa ilk IP gerçek istemci IP'sidir
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * HTTP 429 yanıtını ErrorResponse formatında yazar.
     * GlobalExceptionHandler filter dışında çalıştığından yanıt burada doğrudan yazılır.
     *
     * @param response HTTP yanıtı
     * @throws IOException JSON serializasyon hatası
     */
    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                "Çok fazla istek gönderildi. Lütfen bekleyiniz.",
                ErrorType.RATE_LIMIT_EXCEEDED
        );
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
