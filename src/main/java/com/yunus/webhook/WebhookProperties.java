package com.yunus.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.webhook")
public record WebhookProperties(
        String n8nUrl,
        boolean enabled
) {}
