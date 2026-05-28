package com.yunus.security;

import com.yunus.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Her HTTP isteğinde Authorization header'daki Bearer token'ı doğrulayan filtre.
 * Token geçerliyse SecurityContext'e authentication bilgisi set eder.
 * <p>
 * Redis blacklist davranışı:
 *   app.security.fail-on-redis-blacklist-error=false (dev, default):
 *     Redis erişilemezse blacklist kontrolü atlanır — uygulama ayakta kalır (graceful degradation).
 *   app.security.fail-on-redis-blacklist-error=true (prod):
 *     Redis erişilemezse token geçersiz sayılır — daha güvenli, sistem daha kırılgan.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityProperties securityProperties;

    public JwtFilter(JwtService jwtService,
                     UserDetailsService userDetailsService,
                     RedisTemplate<String, String> redisTemplate,
                     SecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Bearer token yoksa filtre zinciri devam eder
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Redis blacklist kontrolü
            if (isTokenBlacklisted(jwt)) {
                log.debug("Token is blacklisted, rejecting request to: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            final String username = jwtService.extractUsername(jwt);

            // Kullanıcı adı çıkarılamadıysa veya zaten authenticate ise geç
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            // Token geçersizse veya herhangi bir hata olursa sessizce devam et
            log.debug("JWT authentication failed for request to {}: {}", request.getRequestURI(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Token'ın Redis blacklist'te olup olmadığını kontrol eder.
     * <p>
     * Redis bağlantı hatası durumunda davranış SecurityProperties ile yönetilir:
     *   fail-on-redis-blacklist-error=false → false döner (token geçerli sayılır, sistem ayakta kalır)
     *   fail-on-redis-blacklist-error=true  → true döner (token geçersiz sayılır, daha güvenli)
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception ex) {
            if (securityProperties.failOnRedisBlacklistError()) {
                log.error("Redis blacklist check failed and fail-on-error=true, rejecting token: {}", ex.getMessage());
                return true; // Token geçersiz say — daha güvenli
            }
            log.warn("Redis blacklist check failed, proceeding without blacklist check (fail-on-error=false): {}", ex.getMessage());
            return false; // Sistemi ayakta tut — graceful degradation
        }
    }
}
