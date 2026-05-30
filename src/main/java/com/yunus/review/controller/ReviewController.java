package com.yunus.review.controller;

// Sınıf adı: ReviewController
// Amacı: Kullanıcı değerlendirme (yorum + puan) endpoint'lerini sunar.
// Ne yapıyor: İşletmeye yorum yapma, listeleme ve silme işlemleri için
//             HTTP endpoint'leri sağlar. P6 fazında endpoint'ler eklenecek.

import com.yunus.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kullanıcı değerlendirme endpoint'leri.
 *
 * <p><b>P6 fazında endpoint metodları eklenecek.</b>
 *
 * <p>Planlanan endpoint'ler:
 * <ul>
 *   <li>POST   /api/v1/reviews            — değerlendirme oluştur</li>
 *   <li>GET    /api/v1/reviews/business/{id} — işletme değerlendirmeleri</li>
 *   <li>DELETE /api/v1/reviews/{id}       — değerlendirme sil</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Kullanıcı değerlendirme ve puan yönetimi")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // P6 fazında endpoint metodları eklenecek
}
