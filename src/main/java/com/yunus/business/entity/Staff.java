package com.yunus.business.entity;

import com.yunus.common.entity.BaseEntity;
import com.yunus.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * İşletmede hizmet veren personeli temsil eder.
 * Berber, kuaför, uzman doktor, veteriner hekim gibi — sisteme kayıtlı kullanıcı olmayabilir (user nullable).
 */
@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Personel uygulamaya giriş yapıyorsa bağlı kullanıcı; yoksa null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Görünen unvan: "Uzman", "Kıdemli Berber", "Diş Hekimi" vb.
    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // Personel ayrıldığında false; fiziksel silme yapılmaz
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    // Bu personelin hangi hizmetleri verebileceği; slot ve randevu hesaplamasında kullanılır
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_services",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<Service> services = new HashSet<>();

}
