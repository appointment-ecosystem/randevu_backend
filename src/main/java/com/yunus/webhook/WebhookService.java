package com.yunus.webhook;

import java.util.Map;

public interface WebhookService {

    /**
     * n8n webhook URL'ine HTTP POST gönderir.
     * n8n URL boş veya devre dışıysa sessizce atlanır.
     *
     * @param eventType olayı tanımlayan sabit ({@link WebhookEvent})
     * @param payload   olaya özgü veri
     */
    void sendWebhook(String eventType, Map<String, Object> payload);
}
