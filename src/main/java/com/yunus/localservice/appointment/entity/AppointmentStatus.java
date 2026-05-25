package com.yunus.localservice.appointment.entity;

/**
 * Randevunun yaşam döngüsü durumu; fiziksel silme yerine status ile yönetilir.
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED_BY_USER,
    CANCELLED_BY_BUSINESS,
    COMPLETED,
    NO_SHOW,
    EXPIRED
}
