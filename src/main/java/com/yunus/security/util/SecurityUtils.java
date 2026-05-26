package com.yunus.security.util;

import com.yunus.common.exception.UnauthorizedException;
import com.yunus.security.service.CustomUserDetails;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

/**
 * Güvenlik bağlamından (SecurityContext) kullanıcı bilgilerini çekmek için yardımcı sınıf.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Yardımcı sınıf, instance oluşturulamaz
    }

    /**
     * Mevcut oturum açmış kullanıcının CustomUserDetails nesnesini döner.
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Bu işlem için giriş yapılması gerekmektedir");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        throw new UnauthorizedException("Geçersiz kimlik bilgisi oturumu");
    }

    /**
     * Mevcut oturum açmış kullanıcının Entity nesnesini döner.
     */
    public static User getCurrentUser() {
        return getCurrentUserDetails().getUser();
    }

    /**
     * Mevcut oturum açmış kullanıcının UUID'sini döner.
     */
    public static UUID getCurrentUserId() {
        return getCurrentUserDetails().getUserId();
    }

    /**
     * Mevcut oturum açmış kullanıcının rolünü döner.
     */
    public static UserRole getCurrentUserRole() {
        return getCurrentUserDetails().getRole();
    }
}
