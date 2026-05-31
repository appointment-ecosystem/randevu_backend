package com.yunus.auth.dto;

import com.yunus.appointment.entity.AppointmentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin paneli için kullanıcıya ait tek bir randevu özet yanıt DTO'su.
 * İşletme, hizmet ve personel adları snapshot olarak taşınır;
 * entity ilişkilerinden türetilir, doğrudan entity dönülmez.
 */
public record AdminUserAppointmentResponse(

        /** Randevunun benzersiz kimliği. */
        UUID id,

        /** Randevunun yapıldığı işletmenin adı. */
        String businessName,

        /** Randevunun alındığı hizmetin adı. */
        String serviceName,

        /**
         * Randevuyu gerçekleştirecek personelin adı.
         * Personel atanmamış işletmelerde null olabilir.
         */
        String staffName,

        /** Randevunun başlangıç tarihi ve saati. */
        OffsetDateTime startTime,

        /** Randevunun bitiş tarihi ve saati. */
        OffsetDateTime endTime,

        /** Randevunun mevcut durumu (PENDING, CONFIRMED, COMPLETED vb.). */
        AppointmentStatus status,

        /** Randevu oluşturulduğu andaki hizmet fiyatının anlık görüntüsü. */
        BigDecimal priceSnapshot
) {
}
