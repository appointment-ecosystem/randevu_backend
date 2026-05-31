package com.yunus.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metot düzeyinde rate limiting uygulamak için kullanılan özel annotation.
 * RateLimitInterceptor tarafından preHandle aşamasında okunur ve sliding window
 * algoritması ile Redis üzerinden istek sayısı denetlenir.
 *
 * <p>Kullanım örneği:
 * <pre>
 * {@literal @}RateLimit(limit = 5, windowSeconds = 60, key = "auth:login", keyType = KeyType.IP)
 * public ResponseEntity{@literal <}...{@literal >} login(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Pencere süresi içinde izin verilen maksimum istek sayısı.
     * Bu değer aşıldığında HTTP 429 döner.
     */
    int limit();

    /**
     * Sliding window pencere süresi (saniye cinsinden).
     * Redis key'in TTL'si de bu değere ayarlanır.
     */
    int windowSeconds();

    /**
     * Redis anahtarının endpoint bölümünü oluşturan sabit string.
     * Örn: "auth:login", "auth:register", "discover"
     * Nihai key formatı: rate_limit:{key}:{identifier}
     */
    String key();

    /**
     * Anahtar identifier türü: IP adresi mi yoksa kullanıcı ID'si mi kullanılacak.
     * Varsayılan: IP
     */
    KeyType keyType() default KeyType.IP;
}
