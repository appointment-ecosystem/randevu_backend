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
 * İlçe kaydı; bir ile bağlıdır. Filtreleme ve işletme adresi için kullanılır.
 */
@Entity
@Table(
        name = "districts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"city_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
public class District extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

}
