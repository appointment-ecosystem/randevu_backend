package com.yunus.payment.entity;

/**
 * Ödeme işleminin gateway ve iş akışı durumu.
 */
public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
