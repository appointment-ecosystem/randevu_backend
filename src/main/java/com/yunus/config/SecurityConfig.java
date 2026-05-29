package com.yunus.config;

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

/**
 * Spring Security temel yapılandırma sınıfı.
 * CSRF kapalı, session yönetimi stateless (JWT tabanlı).
 * <p>
 * Public endpoint'ler (authentication gerekmez):
 *   POST /api/v1/auth/register
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/refresh
 * <p>
 * Authenticated endpoint'ler:
 *   POST /api/v1/auth/logout
 *   GET  /api/v1/auth/me
 *   ... (diğer tüm endpoint'ler)
 * <p>
 * CORS: izin verilen origin'ler CorsProperties üzerinden application.yml'den okunur.
 * Dev: localhost portları. Prod: gerçek frontend domain'leri.
 */
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
                        // Public auth endpoint'leri — sadece belirtilen 3 path
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        // Diğer GET public endpoint'ler
                        .requestMatchers(HttpMethod.GET, "/api/v1/businesses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/locations/**").permitAll()
                        // Kullanıcı profil endpoint'leri — USER, BUSINESS_OWNER, ADMIN
                        .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**")
                        .hasAnyRole("USER", "BUSINESS_OWNER", "ADMIN")
                        // Swagger / OpenAPI — prod'da SwaggerConfig @Profile("!prod") ile devre dışı
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Diğer tüm endpoint'ler — authentication zorunlu
                        // logout (/api/v1/auth/logout) ve me (/api/v1/auth/me) buraya dahil
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

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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
