package com.yunus.business.service;

import com.yunus.business.dto.HolidayRequest;
import com.yunus.business.dto.HolidayResponse;
import java.util.List;
import java.util.UUID;

/**
 * İşletme ve personel tatil/kapalı gün yönetimi için servis sözleşmesi.
 */
public interface HolidayService {

    /**
     * Tatilleri listeler.
     * staffId null ise işletme geneli tatiller; dolu ise personele özel tatiller.
     */
    List<HolidayResponse> getHolidays(UUID businessId, UUID staffId);

    /**
     * Yeni tatil ekler; aynı güne aynı kapsam için tekrar eklenemez.
     */
    HolidayResponse addHoliday(UUID businessId, HolidayRequest request);

    /**
     * Belirtilen tatili siler; yalnızca işletme sahibi yapabilir.
     */
    void deleteHoliday(UUID businessId, UUID holidayId);
}
