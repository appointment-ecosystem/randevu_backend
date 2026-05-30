package com.yunus.business.service;

// Sınıf adı: OpenStatusService
// Amacı: İşletmenin anlık açık/kapalı durum sorgu sözleşmesini tanımlar.
// Ne yapıyor: Bugünkü tatil ve çalışma saati verilerini kontrol ederek
//             işletmenin şu an açık olup olmadığını dönen tek metod bildirir.

import com.yunus.business.dto.OpenStatusResponse;
import java.util.UUID;

/**
 * İşletme açık/kapalı durum servis sözleşmesi.
 *
 * <p>Kontrol sırası: tatil → çalışma saati tanımlı mı → isClosed → saat aralığı.
 */
public interface OpenStatusService {

    /**
     * Belirtilen işletmenin şu anki açık/kapalı durumunu döner.
     *
     * @param businessId işletme UUID'si
     * @return anlık durum yanıtı
     * @throws com.yunus.common.exception.ResourceNotFoundException işletme bulunamazsa
     */
    OpenStatusResponse getOpenStatus(UUID businessId);
}
