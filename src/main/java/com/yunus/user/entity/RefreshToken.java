package com.yunus.user.entity;

import com.yunus.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JWT yenileme (refresh) oturumlarını saklar.
 * Ham token değil, yalnızca hash tutulur; iptal işlemi service katmanında yönetilir.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // İstemciden gelen refresh token'ın hash'i; düz metin saklanmaz
    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // true ise token geçersiz sayılır (logout / güvenlik iptali)
    @Column(nullable = false)
    private Boolean revoked = false;

}
