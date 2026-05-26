package com.yunus.security.service;

import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security'nin UserDetails arayüzünü implemente eden wrapper sınıf.
 * User entity'sini sarararak güvenlik bağlamında kullanılabilir hale getirir.
 * Authority üretiminde ROLE_ prefix otomatik eklenir.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ prefix sistem içinde üretilir; enum'da saklanmaz
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        // Telefon numarası ana kimlik doğrulama kanalıdır
        return user.getPhone();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // isActive false ise hesap devre dışı sayılır
        return user.getIsActive();
    }

    // User entity'sine erişim — security context'ten kullanıcı bilgileri alınır
    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public UserRole getRole() {
        return user.getRole();
    }
}
