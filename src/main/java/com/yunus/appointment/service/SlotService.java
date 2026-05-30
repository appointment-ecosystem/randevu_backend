package com.yunus.appointment.service;

import com.yunus.appointment.dto.AvailableSlotsRequest;
import com.yunus.appointment.dto.SlotResponse;
import java.util.List;

/**
 * Randevu alınabilir zaman dilimlerini (slot) hesaplayan servis sözleşmesi.
 *
 * <p>Slot hesaplama; çalışma saatleri, tatil günleri, mevcut randevular ve
 * Redis slot kilitleri birlikte değerlendirilerek yapılır.
 */
public interface SlotService {

    /**
     * Verilen istek parametrelerine göre müsait ve dolu slotları hesaplayıp döner.
     *
     * <p>Kullanım: {@code POST /api/v1/appointments/available-slots}
     *
     * @param request işletme, hizmet, isteğe bağlı personel ve tarih bilgisi
     * @return slot listesi; müsait olanlar {@code available = true}, dolu/kilitliler {@code false}
     */
    List<SlotResponse> getAvailableSlots(AvailableSlotsRequest request);
}
