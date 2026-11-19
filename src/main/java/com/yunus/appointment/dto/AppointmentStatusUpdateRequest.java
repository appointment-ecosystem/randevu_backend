package com.yunus.appointment.dto;

import com.yunus.appointment.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Mevcut bir randevunun durumunu güncellemek için kullanılan istek nesnesi.
 */
public record AppointmentStatusUpdateRequest(
        @NotNull AppointmentStatus newStatus,
        String reason
) {}
