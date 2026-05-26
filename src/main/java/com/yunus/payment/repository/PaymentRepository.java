package com.yunus.payment.repository;

import com.yunus.payment.entity.Payment;
import com.yunus.payment.entity.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ödeme kayıtları; randevu ve kullanıcı bazlı sorgular (Faz 11 entegrasyonu).
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByAppointmentId(UUID appointmentId);

    List<Payment> findByUserId(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

}
