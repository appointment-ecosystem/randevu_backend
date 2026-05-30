package com.yunus.appointment.controller;

import com.yunus.appointment.dto.AvailableSlotsRequest;
import com.yunus.appointment.dto.SlotResponse;
import com.yunus.appointment.service.SlotService;
import com.yunus.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Randevu alınabilir zaman dilimlerini (slot) sorgulama endpoint'leri.
 *
 * <p>Tüm endpoint'ler kimlik doğrulaması gerektirir.
 * SecurityConfig'deki {@code .anyRequest().authenticated()} kuralı kapsamındadır;
 * ek {@code requestMatchers} eklemeye gerek yoktur.
 */
@RestController
@RequestMapping("/api/v1/slots")
@Tag(name = "Slots", description = "Müsait slot sorgulama")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /**
     * Belirtilen işletme, hizmet, personel (opsiyonel) ve tarihe göre
     * müsait randevu zaman dilimlerini listeler.
     *
     * <p>GET yerine POST tercih edildi: tarih, hizmet ve personel gibi
     * birden fazla filtre parametresi request body ile temiz biçimde iletilir.
     *
     * <p>{@code POST /api/v1/slots/available}
     *
     * @param request işletme, hizmet, personel (nullable) ve tarih bilgisi
     * @return müsait ve dolu slotların listesi
     */
    @PostMapping("/available")
    @Operation(summary = "Müsait slotları listele",
               description = "Seçili işletme, hizmet ve tarihe göre randevu alınabilecek " +
                             "zaman dilimlerini döner. staffId opsiyoneldir.")
    public ResponseEntity<BaseResponse<List<SlotResponse>>> getAvailableSlots(
            @Valid @RequestBody AvailableSlotsRequest request) {

        List<SlotResponse> slots = slotService.getAvailableSlots(request);
        return ResponseEntity.ok(BaseResponse.success(slots));
    }
}
