package com.yunus.security;

import com.yunus.security.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * JWT token üretme, doğrulama ve claim çözümleme işlemlerini yürüten servis.
 * JJWT 0.12.x API ile HS256 algoritması kullanır.
 */
@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /** Access token üretir — sub: phone, role ve userId claim eklenir */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // UserPrincipal üzerinden role bilgisi alınır
        if (userDetails instanceof UserPrincipal userPrincipal) {
            extraClaims.put("role", userPrincipal.getRole().name());
            extraClaims.put("userId", userPrincipal.getUserId().toString());
        }
        return buildToken(extraClaims, userDetails, jwtProperties.accessTokenExpiration());
    }

    /** Refresh token üretir — daha uzun ömürlü */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtProperties.refreshTokenExpiration());
    }

    /** Token'dan kullanıcı adını (phone) çıkarır */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Token'dan belirli bir claim değeri çıkarır */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /** Token geçerliliğini doğrular — kullanıcı adı eşleşmeli ve süre dolmamış olmalı */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /** Token süresinin dolup dolmadığını kontrol eder */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Token'dan userId claim değerini güvenli biçimde çeker.
     * Token geçersiz, süresi dolmuş veya userId claim'i yoksa null döner.
     * Rate limiter gibi kritik olmayan bileşenler tarafından kullanılır;
     * exception fırlatmak yerine null ile IP'ye düşülür.
     *
     * @param token Ham JWT string (Bearer prefix olmadan)
     * @return userId string değeri; herhangi bir hata durumunda null
     */
    public String extractUserIdSafe(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userId = claims.get("userId");
            return userId != null ? userId.toString() : null;
        } catch (Exception e) {
            // Geçersiz token, süresi dolmuş token veya parse hatası — null dön
            return null;
        }
    }

    /** Secret key'i decode edip HMAC-SHA anahtarı oluşturur */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
