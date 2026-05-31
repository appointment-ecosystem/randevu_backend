package com.yunus.business.dto;

import jakarta.validation.constraints.Size;

/**
 * Admin panelinde bir işletmenin onay veya aktiflik durumunu güncellemek için kullanılan istek verisi.
 * Durum değişikliği REJECTED (Reddedildi) veya SUSPENDED (Askıya alındı) ise gerekçe belirtilmesi zorunludur.
 */
public record AdminBusinessStatusUpdateRequest(

        /**
         * Durum değişikliği gerekçesi (Reddedildi ve Askıya alındı durumlarında zorunludur).
         * Onaylama ve aktifleştirme işlemlerinde boş bırakılabilir veya kullanılmaz.
         */
        @Size(max = 1000, message = "Gerekçe en fazla 1000 karakter olabilir")
        String reason
) {
}
