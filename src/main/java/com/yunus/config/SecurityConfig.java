package com.yunus.config;

/**
 * Spring Security temel yapılandırma sınıfı.
 * CSRF kapalı, session yönetimi stateless (JWT tabanlı).
 *
 * ─── Public Endpoint'ler (authentication gerekmez) ───────────────────────────
 *
 * Auth:
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/refresh
 *
 * Ödeme (payments) — iyzico callback, Faz 11:
 *   POST /api/v1/payments/callback
 *
 * Konum (location):
 *   GET  /api/v1/locations/**
 *
 * Kategoriler (categories):
 *   GET  /api/v1/categories
 *
 * İşletme (businesses) — GET public:
 *   GET  /api/v1/businesses
 *   GET  /api/v1/businesses/{id}
 *   GET  /api/v1/businesses/{id}/services
 *   GET  /api/v1/businesses/{id}/services/{serviceId}
 *   GET  /api/v1/businesses/{id}/staff
 *   GET  /api/v1/businesses/{id}/staff/{staffId}
 *   GET  /api/v1/businesses/{id}/photos
 *   GET  /api/v1/businesses/{id}/working-hours
 *   GET  /api/v1/businesses/{id}/working-hours/staff/{staffId}
 *
 * Keşfet (discover) — Faz 5:
 *   GET  /api/v1/discover/businesses
 *   GET  /api/v1/discover/businesses/{id}
 *   GET  /api/v1/discover/businesses/search
 *   GET  /api/v1/discover/businesses/{id}/open-status
 *
 * Değerlendirme (review) — Faz 5:
 *   GET  /api/v1/reviews/business/{businessId}
 *
 * Swagger / OpenAPI:
 *   /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html
 *
 * ─── Kimlik Doğrulama Gerektiren Endpoint'ler ────────────────────────────────
 *
 * Kullanıcı profili:
 *   ANY  /api/v1/users/me, /api/v1/users/me/**  → USER, BUSINESS_OWNER, BUSINESS_EMPLOYEE, ADMIN
 *
 * Değerlendirme (review) — Faz 5:
 *   POST   /api/v1/reviews
 *   GET    /api/v1/reviews/my
 *   DELETE /api/v1/reviews/{reviewId}
 *
 * Admin işletme yönetimi:
 *   ANY  /api/v1/admin/businesses/**  → ADMIN
 *
 * Admin kategori yönetimi — P3:
 *   ANY  /api/v1/admin/categories/**  → ADMIN
 *
 * Admin kullanıcı yönetimi — P4:
 *   ANY  /api/v1/admin/users/**  → ADMIN
 *
 * Admin yorum yönetimi — P5:
 *   ANY  /api/v1/admin/reviews/**  → ADMIN
 *
 * Diğer tüm endpoint'ler: authenticated()
 *
 * ─── CORS ────────────────────────────────────────────────────────────────────
 *   İzin verilen origin'ler CorsProperties üzerinden application.yml'den okunur.
 *   Dev: localhost portları. Prod: gerçek frontend domain'leri.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yunus.common.response.BaseResponse;
import com.yunus.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtFilter jwtFilter,
                          UserDetailsService userDetailsService,
                          CorsProperties corsProperties) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth

                        // ── Auth (public) ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                        // ── Konum (public) ───────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()

                        // ── Kategoriler (public) ─────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/business-categories/**").permitAll()

                        // ── Randevular (public) ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/available-slots").permitAll()

                        // ── Ödeme callback (public) — iyzico tarafından çağrılır ─────
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/callback").permitAll()

                        // ── İşletme — legacy (public) ────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/businesses/**").permitAll()

                        // ── Keşfet / Discover (public) — Faz 5 ──────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/discover/businesses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/discover/businesses/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/discover/businesses/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/discover/businesses/{id}/open-status").permitAll()

                        // ── Değerlendirme (public) — Faz 5 ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/business/{businessId}").permitAll()

                        // ── Swagger / OpenAPI (public) ───────────────────────────────
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ── Admin İşletme Yönetimi (rol bazlı) ─────────────────────────
                        .requestMatchers("/api/v1/admin/businesses", "/api/v1/admin/businesses/**")
                        .hasRole("ADMIN")

                        // ── Admin Kategori Yönetimi (rol bazlı) — P3 ────────────────────
                        .requestMatchers("/api/v1/admin/categories", "/api/v1/admin/categories/**")
                        .hasRole("ADMIN")

                        // ── Admin Kullanıcı Yönetimi (rol bazlı) — P4 ───────────────────
                        .requestMatchers("/api/v1/admin/users", "/api/v1/admin/users/**")
                        .hasRole("ADMIN")

                        // ── Admin Yorum Yönetimi (rol bazlı) — P5 ───────────────────────
                        .requestMatchers("/api/v1/admin/reviews", "/api/v1/admin/reviews/**")
                        .hasRole("ADMIN")

                        // ── Kullanıcı profili (rol bazlı) ────────────────────────────
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**")
                        .hasAnyRole("USER", "BUSINESS_OWNER", "BUSINESS_EMPLOYEE", "ADMIN")

                        // ── Randevu işlemleri (açıkça authenticated) ─────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/user/{userId}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/{id}/cancel-by-user").authenticated()

                        // ── Değerlendirme (kimlik doğrulama gerekli) — Faz 5 ─────────
                        .requestMatchers(HttpMethod.POST,   "/api/v1/reviews").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/v1/reviews/my").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/{reviewId}").authenticated()

                        // ── Device token endpoint'leri — kimlik doğrulama gerektirir
                        .requestMatchers("/api/v1/device-tokens/**").authenticated()

                        // ── Diğer tüm endpoint'ler — kimlik doğrulama zorunlu ────────
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS yapılandırması.
     * İzin verilen origin'ler CorsProperties üzerinden application.yml'den okunur.
     * allowCredentials(true) ve wildcard origin birlikte kullanılmaz — tarayıcı engeller.
     * Dev'de localhost portları yeterli; prod'da application-prod.yml üzerinden gerçek domain'ler verilir.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = corsProperties.allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            log.warn("No CORS allowed origins configured under app.cors.allowed-origins — defaulting to localhost:3000");
            origins = List.of("http://localhost:3000");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        // iyzico 3DS callback: bankanın hosted ödeme sayfası farklı bir origin'den
        // (form POST ile, Origin header'ı set edilmiş şekilde) bu endpoint'e POST yapar.
        // Spring'in CorsFilter'ı, /** için tanımlı allowedOrigins listesinde olmayan
        // origin'leri preflight olmayan isteklerde de reddeder (403). Bu yüzden bu
        // path için ayrı, kimlik bilgisi gerektirmeyen, tüm origin'lere izin veren
        // bir konfigürasyon tanımlanır.
        CorsConfiguration callbackConfiguration = new CorsConfiguration();
        callbackConfiguration.setAllowedOrigins(List.of("*"));
        callbackConfiguration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        callbackConfiguration.setAllowedHeaders(List.of("*"));
        callbackConfiguration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/payments/callback", callbackConfiguration);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Kimlik doğrulanmamış isteklerde JSON formatında 401 yanıtı döner.
     * Filter chain'den dönen hata olduğu için GlobalExceptionHandler yerine burada yönetilir.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            log.warn("Unauthorized request to: {}", request.getRequestURI());
            writeJsonResponse(response, HttpStatus.UNAUTHORIZED, "Kimlik doğrulama gerekli");
        };
    }

    /**
     * Yetkilendirme başarısız olduğunda JSON formatında 403 yanıtı döner.
     * Filter chain'den dönen hata olduğu için GlobalExceptionHandler yerine burada yönetilir.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied for request to: {}", request.getRequestURI());
            writeJsonResponse(response, HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz bulunmamaktadır");
        };
    }

    // JSON formatında HTTP yanıtı yazan yardımcı metot
    private void writeJsonResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        BaseResponse<Void> body = BaseResponse.error(message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
