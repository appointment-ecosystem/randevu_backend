package com.yunus.payment.entity;

import com.yunus.appointment.entity.Appointment;
import com.yunus.common.entity.BaseEntity;
import com.yunus.payment.enums.PaymentStatus;
import com.yunus.payment.enums.PaymentType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * iyzico üzerinden gerçekleştirilen ödeme kaydı.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    // Ödemenin ilişkili olduğu randevu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    // Ödemeyi yapan kullanıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ödeme tutarı
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "TRY";

    // DEPOSIT (kapora) veya FULL (tam ödeme)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    // Ödeme sağlayıcısı (varsayılan: IYZICO)
    @Column(nullable = false, length = 30)
    private String provider = "IYZICO";

    // iyzico'nun döndürdüğü paymentId
    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    // İade için gereken paymentTransactionId
    @Column(name = "provider_transaction_reference", length = 255)
    private String providerTransactionReference;

    // iyzico ham response - kart bilgisi ASLA içermez
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private String providerResponse;

}
