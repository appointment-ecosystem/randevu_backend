package com.yunus.localservice.business.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İşletmenin sunduğu randevu alınabilir hizmeti (saç kesimi, muayene, bakım vb.).
 * Spring @Service bean'i ile karışmaması için business.entity paketinde tutulur.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class Service extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Randevu slot süresi hesabında kullanılır (dakika)
    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    // Güncel liste fiyatı; randevu anındaki fiyat Appointment.priceSnapshot'ta saklanır
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency = "TRY";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // false = listeden kalkar; kayıt silinmez
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

}
