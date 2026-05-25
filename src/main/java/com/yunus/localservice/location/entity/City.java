package com.yunus.localservice.location.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Türkiye illeri; seed data ile yüklenir, admin panelden yönetilir.
 * Serbest metin şehir girişi yerine FK ile kullanılır.
 */
@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
public class City extends BaseEntity {

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    // Plaka kodu (örn. "35" İzmir)
    @Column(length = 10)
    private String code;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

}
