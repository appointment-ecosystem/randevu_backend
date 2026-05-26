package com.yunus.business.entity;

import com.yunus.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İşletme türü / sektör kategorisi (Berber, Kuaför, Veteriner vb.).
 * Admin tarafından yönetilir; slug ile API filtrelemesinde kullanılır.
 */
@Entity
@Table(name = "business_categories")
@Getter
@Setter
@NoArgsConstructor
public class BusinessCategory extends BaseEntity {

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    // URL ve sorgu parametresi (örn. ?category=berber)
    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

}
