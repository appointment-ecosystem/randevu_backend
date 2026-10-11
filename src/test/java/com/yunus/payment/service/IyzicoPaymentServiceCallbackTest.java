package com.yunus.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.Options;
import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.payment.config.IyzicoProperties;
import com.yunus.payment.entity.Payment;
import com.yunus.payment.enums.PaymentStatus;
import com.yunus.payment.repository.PaymentRepository;
import com.yunus.payment.service.impl.IyzicoPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IyzicoPaymentServiceCallbackTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private Options iyzicoOptions;

    @Mock
    private IyzicoProperties iyzicoProperties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IyzicoPaymentServiceImpl paymentService;

    private final String frontendUrl = "http://localhost:3000";
    private UUID paymentId;
    private Payment payment;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "frontendUrl", frontendUrl);

        paymentId = UUID.randomUUID();
        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        payment = new Payment();
        payment.setId(paymentId);
        payment.setAppointment(appointment);
    }

    @Test
    @DisplayName("Idempotent Callback: Payment already SUCCESS -> return success URL immediately without modifying or calling external API")
    void handleCallback_whenPaymentAlreadySuccess_shouldReturnSuccessImmediatelyAndNotMutate() {
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Map<String, String> callbackParams = Map.of(
                "conversationId", paymentId.toString(),
                "paymentId", "iyzico-12345",
                "conversationData", "some-data",
                "mdStatus", "1"
        );

        String result = paymentService.handleCallback(callbackParams);

        assertEquals(frontendUrl + "/odeme/sonuc?status=basarili&id=" + paymentId, result);

        // Verify that paymentRepository.save or appointmentRepository.save was NEVER called again
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(appointmentRepository, never()).save(any(Appointment.class));
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    @DisplayName("Callback with null conversationId should redirect to error")
    void handleCallback_whenConversationIdNull_shouldReturnError() {
        Map<String, String> callbackParams = Map.of(
                "paymentId", "iyzico-12345"
        );

        String result = paymentService.handleCallback(callbackParams);

        assertEquals(frontendUrl + "/odeme/sonuc?status=hata", result);
        verifyNoInteractions(paymentRepository);
    }
}