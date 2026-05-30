package com.yunus.business.controller;

// Sınıf adı: DiscoverController
// Amacı: İşletme keşif ve yakın işletme arama endpoint'lerini sunar.
// Ne yapıyor: Konum parametresi (lat, lon, radius) ve isteğe bağlı kategori/hizmet
//             filtresiyle işletme listesi döner. P5 fazında endpoint'ler eklenecek.

import com.yunus.business.service.BusinessDiscoveryService;
import com.yunus.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * İşletme keşif endpoint'leri.
 *
 * <p><b>P5 fazında endpoint metodları eklenecek.</b>
 *
 * <p>Planlanan endpoint'ler:
 * <ul>
 *   <li>GET /api/v1/discover/nearby — konum bazlı yakın işletmeler</li>
 *   <li>GET /api/v1/discover/search — anahtar kelime + kategori araması</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/discover")
@Tag(name = "Discover", description = "İşletme keşif ve yakın işletme arama")
@RequiredArgsConstructor
public class DiscoverController {

    private final BusinessDiscoveryService businessDiscoveryService;

    // P5 fazında endpoint metodları eklenecek
}
