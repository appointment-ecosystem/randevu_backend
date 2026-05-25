package com.yunus.localservice.business.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import com.yunus.localservice.location.entity.City;
import com.yunus.localservice.location.entity.District;
import com.yunus.localservice.location.entity.Neighborhood;
import com.yunus.localservice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Randevu alınabilen işletmeyi temsil eder (berber, klinik, kuaför vb. kategori bağımsız).
 * Konum, onay durumu ve kategoriler bu entity üzerinden yönetilir.
 */
@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
public class Business extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    // SEO ve paylaşılabilir URL için benzersiz kısa ad (örn. "ahmet-berber")
    @Column(nullable = false, length = 150, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "neighborhood_id")
    private Neighborhood neighborhood;

    // Sokak, cadde, bina no gibi detay; il/ilçe FK alanlarından ayrı tutulur
    @Column(name = "address_line", length = 300)
    private String addressLine;

    // Faz 17'de PostGIS'e geçişe uygun; şimdilik Haversine sorguları için
    private Double latitude;

    private Double longitude;

    // Admin onayı ve işletme görünürlüğü (PENDING, APPROVED, PASSIVE vb.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusinessStatus status = BusinessStatus.PENDING;

    // status = REJECTED iken admin gerekçesi
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Bir işletme birden fazla kategoriye bağlanabilir (örn. berber + güzellik)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "business_category_map",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<BusinessCategory> categories = new HashSet<>();

}
