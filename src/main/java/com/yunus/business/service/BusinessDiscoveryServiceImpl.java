package com.yunus.business.service;

// Sınıf adı: BusinessDiscoveryServiceImpl
// Amacı: BusinessDiscoveryService sözleşmesinin somut implementasyonu.
// Ne yapıyor: Konum bazlı işletme arama ve filtreleme mantığını uygular.
//             HaversineUtil ile mesafe hesaplama entegrasyonu P5 fazında yapılacak.

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link BusinessDiscoveryService} implementasyonu.
 *
 * <p><b>P5 fazında tamamlanacak</b> — şu an sadece iskelet.
 *
 * <p>Planlanan bağımlılıklar: BusinessRepository, HaversineUtil,
 * CategoryRepository, ReviewRepository (ortalama puan için).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessDiscoveryServiceImpl implements BusinessDiscoveryService {

    // P5 fazında inject edilecek:
    // private final BusinessRepository businessRepository;
    // private final BusinessCategoryRepository categoryRepository;
}
