package com.yunus.localservice.business.entity;

import com.yunus.localservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * İşletme veya personelin kapalı olduğu günler (tatil, izin, bayram).
 * Randevu slot hesaplamasında dikkate alınır.
 */
@Entity
@Table(name = "holidays")
@Getter
@Setter
@NoArgsConstructor
public class Holiday extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // null = tüm işletme kapalı; dolu = yalnızca o personel müsait değil
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 200)
    private String reason;

}
