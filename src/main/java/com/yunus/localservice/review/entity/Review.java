package com.yunus.localservice.review.entity;

import com.yunus.localservice.appointment.entity.Appointment;
import com.yunus.localservice.business.entity.Business;
import com.yunus.localservice.common.entity.BaseEntity;
import com.yunus.localservice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tamamlanan randevu sonrası kullanıcı değerlendirmesi (puan + yorum).
 * Her randevuya en fazla bir yorum; silme yerine isVisible ile gizlenir.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    // Aynı randevuya ikinci yorum engellenir (unique constraint)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // 1 ile 5 arası; service katmanında ve DB CHECK ile doğrulanır
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Admin moderasyonu: false yapılarak yorum listeden gizlenir
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

}
