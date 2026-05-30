package com.yunus.business.dto;

// Sınıf adı: OpenStatusResponse
// Amacı: İşletmenin şu anki açık/kapalı durumunu ve nedenini taşır.
// Ne yapıyor: Bugünkü çalışma saati kontrolü sonucunu (açık mı, kaçta açılıyor/kapanıyor,
//             neden kapalı) tek bir immutable record içinde döner.

import java.time.LocalTime;

/**
 * İşletme anlık açık/kapalı durum yanıtı.
 *
 * <p>{@code open = true} ise {@code opensAt} ve {@code closesAt} dolu, {@code reason} null olur.
 * <p>{@code open = false} ise {@code reason} dolu; {@code opensAt}/{@code closesAt}
 * yalnızca "Çalışma saati dışı" durumunda dolu, diğer kapalı durumlarda null olur.
 */
public record OpenStatusResponse(

        /** İşletmenin şu an açık olup olmadığı. */
        boolean open,

        /**
         * Bugünkü açılış saati.
         * Açıksa veya "Çalışma saati dışı" ise dolu; tatil/kapalı gün ise null.
         */
        LocalTime opensAt,

        /**
         * Bugünkü kapanış saati.
         * Açıksa veya "Çalışma saati dışı" ise dolu; tatil/kapalı gün ise null.
         */
        LocalTime closesAt,

        /**
         * Kapalı olmanın nedeni.
         * Olası değerler: "Tatil günü", "Çalışma saati tanımlı değil",
         * "Kapalı gün", "Çalışma saati dışı".
         * {@code open = true} ise null.
         */
        String reason

) {}
