package com.yunus.notification.entity;

import com.yunus.common.entity.BaseEntity;
import com.yunus.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Push bildirim (FCM) cihaz token kaydı.
 * Aktif kullanım Faz 10'da; şema erken hazırlanmıştır.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
public class DeviceToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Firebase Cloud Messaging token; aynı token tekrar gelirse güncellenir
    @Column(nullable = false, length = 500, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

}
