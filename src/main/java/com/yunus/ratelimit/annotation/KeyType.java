package com.yunus.ratelimit.annotation;

/**
 * Rate limit anahtarı türünü belirler.
 * IP: İsteği gönderen IP adresi üzerinden limit uygulanır (kimlik doğrulama gerektirmeyen endpoint'ler için uygundur).
 * USER: JWT token'dan elde edilen userId üzerinden limit uygulanır (oturum açmış kullanıcıya özgü limitler için uygundur).
 * USER seçilmiş ancak geçerli token alınamazsa otomatik olarak IP bazlı moda geçilir.
 */
public enum KeyType {

    /** İstekler IP adresi bazında gruplanır. */
    IP,

    /** İstekler oturum açmış kullanıcının UUID'si bazında gruplanır. */
    USER
}
