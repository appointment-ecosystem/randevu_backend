package com.yunus.notification.service.impl;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.yunus.notification.dto.PushNotificationPayload;
import com.yunus.notification.entity.DeviceToken;
import com.yunus.notification.repository.DeviceTokenRepository;
import com.yunus.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FCMNotificationServiceImpl implements NotificationService {

    private final Optional<FirebaseMessaging> firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Async("notificationTaskExecutor")
    @Override
    @Transactional
    public void sendToUser(UUID userId, PushNotificationPayload payload) {
        if (!firebaseEnabled || firebaseMessaging.isEmpty()) {
            log.info("FCM devre dışı - bildirim gönderilmedi, user: {}", userId);
            return;
        }

        List<String> tokens = deviceTokenRepository.findActiveTokensByUserId(userId);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        sendToTokens(tokens, payload);
    }

    @Async("notificationTaskExecutor")
    @Override
    @Transactional
    public void sendToUsers(List<UUID> userIds, PushNotificationPayload payload) {
        if (!firebaseEnabled || firebaseMessaging.isEmpty()) {
            log.info("FCM devre dışı - bildirim gönderilmedi, toplu users: {}", userIds.size());
            return;
        }

        List<String> tokens = deviceTokenRepository.findActiveTokensByUserIds(userIds);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        sendToTokens(tokens, payload);
    }

    private void sendToTokens(List<String> tokens, PushNotificationPayload payload) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(payload.getTitle())
                    .setBody(payload.getBody())
                    .build();

            if (tokens.size() == 1) {
                String token = tokens.get(0);
                Message message = Message.builder()
                        .setToken(token)
                        .setNotification(notification)
                        .putAllData(payload.getData() != null ? payload.getData() : java.util.Map.of())
                        .putData("type", payload.getType() != null ? payload.getType().name() : "")
                        .build();

                try {
                    firebaseMessaging.get().send(message);
                } catch (Exception e) {
                    handleFCMError(e, token);
                }
            } else {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(tokens)
                        .setNotification(notification)
                        .putAllData(payload.getData() != null ? payload.getData() : java.util.Map.of())
                        .putData("type", payload.getType() != null ? payload.getType().name() : "")
                        .build();

                BatchResponse response = firebaseMessaging.get().sendEachForMulticast(message);
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int i = 0; i < responses.size(); i++) {
                        if (!responses.get(i).isSuccessful()) {
                            String token = tokens.get(i);
                            handleFCMError(responses.get(i).getException(), token);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("FCM bildirim gönderim hatası: {}", e.getMessage());
        }
    }

    private void handleFCMError(Exception e, String token) {
        log.warn("Token'a bildirim gönderilemedi. Token: {}, Hata: {}", maskToken(token), e.getMessage());
        String errorCode = "";
        if (e instanceof com.google.firebase.messaging.FirebaseMessagingException fme) {
            errorCode = fme.getMessagingErrorCode() != null ? fme.getMessagingErrorCode().name() : "";
        }
        
        if (errorCode.equals("UNREGISTERED") || errorCode.equals("INVALID_ARGUMENT") || errorCode.equals("NOT_FOUND")) {
            deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
                deviceToken.setActive(false);
                deviceTokenRepository.save(deviceToken);
                log.info("Geçersiz token deaktive edildi: {}", maskToken(token));
            });
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
