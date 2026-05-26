package com.yunus.security;

import com.yunus.common.exception.UnauthorizedException;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Güvenlik bağlamından (SecurityContext) kullanıcı bilgilerini çekmek için servis.
 * SecurityContext'e injection ile erişilir; static yardımcı metodların Spring bean karşılığı.
 */
@Service
public class CurrentUserService {

    /**
     * Mevcut oturum açmış kullanıcının UserPrincipal nesnesini döner.
     */
    public UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Bu işlem için giriş yapılması gerekmektedir");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new UnauthorizedException("Geçersiz kimlik bilgisi oturumu");
    }

    /**
     * Mevcut oturum açmış kullanıcının Entity nesnesini döner.
     */
    public User getCurrentUser() {
        return getCurrentUserPrincipal().getUser();
    }

    /**
     * Mevcut oturum açmış kullanıcının UUID'sini döner.
     */
    public UUID getCurrentUserId() {
        return getCurrentUserPrincipal().getUserId();
    }

    /**
     * Mevcut oturum açmış kullanıcının rolünü döner.
     */
    public UserRole getCurrentUserRole() {
        return getCurrentUserPrincipal().getRole();
    }
}
