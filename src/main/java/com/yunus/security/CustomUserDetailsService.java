package com.yunus.security;

import com.yunus.user.entity.User;
import com.yunus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Telefon numarası veya e-posta ile kullanıcı yükleme işlemini gerçekleştiren servis.
 * Spring Security'nin kimlik doğrulama mekanizmasında kullanılır.
 * Pasif kullanıcılar için giriş engellenir.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gelen değer "@" içeriyorsa e-posta, içermiyorsa telefon numarası olarak kabul edilir.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user;
        if (identifier != null && identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> {
                        log.warn("User not found with email: {}", identifier);
                        return new UsernameNotFoundException("Kullanıcı bulunamadı: " + identifier);
                    });
        } else {
            user = userRepository.findByPhone(identifier)
                    .orElseThrow(() -> {
                        log.warn("User not found with phone: {}", identifier);
                        return new UsernameNotFoundException("Kullanıcı bulunamadı: " + identifier);
                    });
        }

        // Pasif kullanıcılar için isEnabled() false dönecek, Spring Security login'i engelleyecek
        return new UserPrincipal(user);
    }
}