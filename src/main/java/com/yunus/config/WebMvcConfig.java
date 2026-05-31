package com.yunus.config;

import com.yunus.ratelimit.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC yapılandırma sınıfı.
 * RateLimitInterceptor'ı tüm endpoint'lere kaydeder.
 * Interceptor, @RateLimit annotation taşımayan metodlara geldiğinde
 * hemen true dönerek ek maliyet yaratmaz.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Bağımlılıkları constructor üzerinden enjekte eden yapıcı metot.
     *
     * @param rateLimitInterceptor Kayıt edilecek rate limiting interceptor
     */
    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * RateLimitInterceptor'ı tüm URL pattern'lerine uygular.
     * Annotation yoksa interceptor anında geçer; performans etkisi minimumdur.
     *
     * @param registry Spring MVC interceptor kayıt nesnesi
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**");
    }
}
