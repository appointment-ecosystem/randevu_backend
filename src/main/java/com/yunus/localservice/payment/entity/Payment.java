package com.yunus.localservice.payment.entity;

import com.yunus.localservice.appointment.entity.Appointment;
import com.yunus.localservice.common.entity.BaseEntity;
import com.yunus.localservice.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Randevuya bağlı ödeme işlemi (kapora veya tam ödeme).
 * Aktif entegrasyon Faz 11'de; provider webhook verisi JSONB olarak saklanır.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "TRY";

    // DEPOSIT = kapora, FULL = tam ödeme
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentProvider provider;

    // Ödeme sağlayıcısındaki işlem / sipariş referans numarası
    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    // Iyzico / PayTR webhook ham JSON yanıtı
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private String providerResponse;

}
