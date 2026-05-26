package com.yunus.appointment.entity;

import com.yunus.business.entity.Business;
import com.yunus.business.entity.Service;
import com.yunus.business.entity.Staff;
import com.yunus.common.entity.BaseEntity;
import com.yunus.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kullanıcı ile işletme arasındaki randevu kaydı.
 * Çakışma önleme: Redis slot lock + DB unique constraint (staff_id + start_time).
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    // Personel seçimi zorunlu olmayan işletmelerde null kalabilir
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    // Randevu oluşturulduğu andaki hizmet fiyatı; sonradan Service.price değişse bile sabit kalır
    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(nullable = false, length = 10)
    private String currency = "TRY";

    @Column(columnDefinition = "TEXT")
    private String notes;

    // İptal durumlarında (CANCELLED_BY_*) doldurulur
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

}
