package com.yunus.webhook.impl;

import com.yunus.webhook.WebhookProperties;
import com.yunus.webhook.WebhookService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class N8nWebhookServiceImpl implements WebhookService {

    private final WebhookProperties webhookProperties;
    private final RestClient restClient;

    @Async("notificationTaskExecutor")
    @Override
    public void sendWebhook(String eventType, Map<String, Object> payload) {
        if (!webhookProperties.enabled()) {
            return;
        }
        String url = webhookProperties.n8nUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("event", eventType);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("data", payload);

        try {
            restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Webhook gönderildi [event={}]", eventType);
        } catch (Exception e) {
            log.warn("Webhook gönderilemedi [event={}, url={}]: {}", eventType, url, e.getMessage());
        }
    }
}
