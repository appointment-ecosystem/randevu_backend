package com.yunus.business.service;

// Sınıf adı: BusinessDiscoveryService
// Amacı: İşletme keşif ve arama işlemlerinin servis sözleşmesini tanımlar.
// Ne yapıyor: Konum bazlı filtreleme, kategori/hizmet bazlı arama ve
//             sıralama (mesafe, puan) gibi keşif metodlarını bildirir.
//             P5 fazında implement edilecek.

import com.yunus.business.dto.BusinessDiscoveryResponse;
import java.util.List;

/**
 * İşletme keşif ve yakın işletme arama servis sözleşmesi.
 *
 * <p><b>P5 fazında metodlar eklenecek.</b>
 */
public interface BusinessDiscoveryService {

    // P5 fazında eklenecek örnek metodlar:
    // List<BusinessDiscoveryResponse> findNearby(double lat, double lon, double radiusKm, UUID categoryId);
    // List<BusinessDiscoveryResponse> search(String query, UUID categoryId, int page, int size);
}
