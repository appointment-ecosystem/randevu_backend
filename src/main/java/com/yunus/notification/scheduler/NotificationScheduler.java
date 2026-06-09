package com.yunus.notification.scheduler;

import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.notification.dto.PushNotificationPayload;
import com.yunus.notification.entity.NotificationType;
import com.yunus.notification.service.NotificationService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Randevu hatırlatma ve yorum isteme bildirimlerini periyodik olarak gönderir.
 * Her 15 dakikada bir çalışır; duplicate önleme Redis key kontrolü ile sağlanır.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final DateTimeFormatter TR_FORMATTER = DateTimeFormatter
            .ofPattern("d MMMM yyyy EEEE HH:mm", Locale.forLanguageTag("tr-TR"));

    private static final long DEDUP_TTL_HOURS = 48;

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 24 saat sonra başlayacak randevular için hatırlatma bildirimi gönderir.
     * 15 dakikalık pencere içindeki randevuları tarar.
     */
    @Async("notificationTaskExecutor")
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void sendAppointmentReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.plusHours(24);
        OffsetDateTime windowEnd = windowStart.plusMinutes(15);

        List<Appointment> appointments = appointmentRepository
                .findAppointmentsForReminder(windowStart, windowEnd);

        for (Appointment appointment : appointments) {
            String dedupKey = "notif:reminder:" + appointment.getId();
            try {
                Boolean isNew = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
                if (Boolean.FALSE.equals(isNew)) {
                    continue;
                }

                String formattedTime = appointment.getStartTime().format(TR_FORMATTER);
                String businessName = appointment.getBusiness().getName();
                String serviceName = appointment.getService().getName();

                PushNotificationPayload payload = PushNotificationPayload.builder()
                        .title("Randevu Hatırlatması")
                        .body(businessName + " — " + serviceName + " randevunuz " + formattedTime + " tarihinde.")
                        .type(NotificationType.APPOINTMENT_REMINDER)
                        .data(Map.of(
                                "appointmentId", appointment.getId().toString(),
                                "type", NotificationType.APPOINTMENT_REMINDER.name()
                        ))
                        .build();

                notificationService.sendToUser(appointment.getUser().getId(), payload);
            } catch (Exception e) {
                log.warn("Hatırlatma bildirimi gönderilemedi [appointmentId={}]: {}",
                        appointment.getId(), e.getMessage());
                redisTemplate.delete(dedupKey);
            }
        }
    }

    /**
     * 1 saat önce tamamlanan randevular için yorum isteme bildirimi gönderir.
     * 15 dakikalık pencere içindeki tamamlanmış randevuları tarar.
     */
    @Async("notificationTaskExecutor")
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void sendReviewRequests() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.minusHours(1);
        OffsetDateTime windowStart = windowEnd.minusMinutes(15);

        List<Appointment> appointments = appointmentRepository
                .findCompletedAppointmentsForReview(windowStart, windowEnd);

        for (Appointment appointment : appointments) {
            String dedupKey = "notif:review:" + appointment.getId();
            try {
                Boolean isNew = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
                if (Boolean.FALSE.equals(isNew)) {
                    continue;
                }

                String businessName = appointment.getBusiness().getName();
                String serviceName = appointment.getService().getName();

                PushNotificationPayload payload = PushNotificationPayload.builder()
                        .title("Deneyiminizi Değerlendirin")
                        .body(businessName + " — " + serviceName + " hizmetini nasıl buldunuz? Görüşünüz bizim için değerli.")
                        .type(NotificationType.REVIEW_REQUEST)
                        .data(Map.of(
                                "appointmentId", appointment.getId().toString(),
                                "businessId", appointment.getBusiness().getId().toString(),
                                "type", NotificationType.REVIEW_REQUEST.name()
                        ))
                        .build();

                notificationService.sendToUser(appointment.getUser().getId(), payload);
            } catch (Exception e) {
                log.warn("Yorum isteme bildirimi gönderilemedi [appointmentId={}]: {}",
                        appointment.getId(), e.getMessage());
                redisTemplate.delete(dedupKey);
            }
        }
    }
}
