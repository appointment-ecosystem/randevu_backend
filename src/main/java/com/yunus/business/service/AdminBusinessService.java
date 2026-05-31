package com.yunus.business.service;

import com.yunus.business.dto.AdminBusinessDetailResponse;
import com.yunus.business.dto.AdminBusinessListResponse;
import com.yunus.business.entity.BusinessStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin paneli üzerinden işletmelerin onay, askıya alma, listeleme ve detay
 * süreçlerini yöneten servis arayüzü.
 */
public interface AdminBusinessService {

    /**
     * İşletmeleri onay durumuna göre sayfalanmış olarak listeler.
     * Eğer durum (status) belirtilmemişse (null ise) tüm işletmeleri getirir.
     *
     * @param status Filtrelenecek işletme onay durumu (opsiyonel)
     * @param pageable Sayfalama ve sıralama parametreleri
     * @return Sayfalanmış işletme listesi
     */
    Page<AdminBusinessListResponse> getAllBusinesses(BusinessStatus status, Pageable pageable);

    /**
     * Kimlik numarası (ID) belirtilen işletmenin tüm admin detay bilgilerini getirir.
     *
     * @param id İşletme kimliği
     * @return İşletme detay yanıt DTO'su
     */
    AdminBusinessDetailResponse getBusinessDetail(UUID id);

    /**
     * İşletmeyi onaylar (Durumu APPROVED yapar, varsa önceki red gerekçesini temizler).
     *
     * @param id Onaylanacak işletme kimliği
     */
    void approveBusiness(UUID id);

    /**
     * İşletmeyi reddeder (Durumu REJECTED yapar ve bir gerekçe kaydeder).
     * Gerekçe (reason) boş veya null ise doğrulama hatası fırlatır.
     *
     * @param id Reddedilecek işletme kimliği
     * @param reason Reddedilme gerekçesi (zorunlu)
     */
    void rejectBusiness(UUID id, String reason);

    /**
     * İşletmeyi askıya alır (Durumu SUSPENDED yapar ve askıya alma gerekçesi kaydeder).
     * Gerekçe (reason) boş veya null ise doğrulama hatası fırlatır.
     *
     * @param id Askıya alınacak işletme kimliği
     * @param reason Askıya alınma gerekçesi (zorunlu)
     */
    void suspendBusiness(UUID id, String reason);

    /**
     * Askıdaki veya pasifteki işletmeyi tekrar aktif/onaylı hale getirir.
     * (Durumu APPROVED yapar, varsa eski gerekçeyi temizler).
     *
     * @param id Aktifleştirilecek işletme kimliği
     */
    void activateBusiness(UUID id);
}
