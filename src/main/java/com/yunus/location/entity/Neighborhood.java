package com.yunus.location.entity;

import com.yunus.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mahalle kaydı; bir ilçeye bağlıdır. İşletme konumunun en ince adres parçasıdır.
 */
@Entity
@Table(
        name = "neighborhoods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"district_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
public class Neighborhood extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

}
