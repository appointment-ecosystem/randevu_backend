package com.yunus.user.entity;

import com.yunus.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sistemdeki tüm kullanıcıları temsil eder (müşteri, işletme sahibi, personel, admin).
 * Tek tabloda role alanı ile ayrılır; fiziksel silme yerine isActive ile pasifleştirilir.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Opsiyonel; telefon ana kimlik doğrulama kanalıdır
    @Column(length = 150, unique = true)
    private String email;

    @Column(nullable = false, length = 20, unique = true)
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    // API yanıtlarında asla dönülmemeli; yalnızca kimlik doğrulama katmanında kullanılır
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    // false yapılarak soft delete uygulanır
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

}
