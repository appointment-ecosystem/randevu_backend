package com.yunus.payment.repository;

import com.yunus.payment.entity.Payment;
import com.yunus.payment.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ödeme kayıtları için veri erişim katmanı.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Bir randevuya ait ödeme kaydını getirir
    Optional<Payment> findByAppointmentId(UUID appointmentId);

    // Bir randevuya ait, belirtilen durumlardan birinde olan ödeme kaydını getirir
    Optional<Payment> findByAppointmentIdAndStatusIn(UUID appointmentId, List<PaymentStatus> statuses);

    // Bir kullanıcıya ait tüm ödeme kayıtlarını getirir
    List<Payment> findAllByUserId(UUID userId);

}
