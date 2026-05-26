package com.yunus.security.service;

import com.yunus.user.entity.User;
import com.yunus.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Telefon numarası ile kullanıcı yükleme işlemini gerçekleştiren servis.
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

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> {
                    log.warn("User not found with phone: {}", phone);
                    return new UsernameNotFoundException("Kullanıcı bulunamadı: " + phone);
                });

        // Pasif kullanıcılar için isEnabled() false dönecek, Spring Security login'i engelleyecek
        return new CustomUserDetails(user);
    }
}
