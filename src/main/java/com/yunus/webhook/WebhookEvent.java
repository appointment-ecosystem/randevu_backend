package com.yunus.webhook;

public final class WebhookEvent {
    public static final String BUSINESS_REGISTERED = "BUSINESS_REGISTERED";
    public static final String BUSINESS_APPROVED   = "BUSINESS_APPROVED";
    public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
    public static final String DAILY_REPORT        = "DAILY_REPORT";

    private WebhookEvent() {}
}
