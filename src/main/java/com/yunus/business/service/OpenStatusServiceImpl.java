package com.yunus.business.service;

// Sınıf adı: OpenStatusServiceImpl
// Amacı: İşletmenin anlık açık/kapalı durumunu kontrol eden servis sınıfıdır.
// Ne yapıyor: Verilen işletme ID'sine göre işletmenin varlığını doğrular, Europe/Istanbul zaman dilimine
//             göre bugünün tarih ve saatini alır, tatil günü ve çalışma saati tanımlarını
//             kontrol ederek anlık açık/kapalı olma durumunu (açık mı, açılış/kapanış saatleri,
//             kapalıysa nedeni) hesaplar.

import com.yunus.business.dto.OpenStatusResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Holiday;
import com.yunus.business.entity.WorkingHour;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.HolidayRepository;
import com.yunus.business.repository.WorkingHourRepository;
import com.yunus.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpenStatusServiceImpl implements OpenStatusService {

    private final BusinessRepository businessRepository;
    private final HolidayRepository holidayRepository;
    private final WorkingHourRepository workingHourRepository;

    @Override
    @Transactional(readOnly = true)
    public OpenStatusResponse getOpenStatus(UUID businessId) {
        // 1. Business'i bul, bulamazsa exception
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));

        // 2. Bugünün tarihini ve saatini al (ZoneId "Europe/Istanbul")
        ZoneId zoneId = ZoneId.of("Europe/Istanbul");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Pazartesi, 7=Pazar

        // 3. Holiday tablosunu kontrol et: business için staff=null olan ve date=bugün olan kayıt var mı?
        Optional<Holiday> holidayOpt = holidayRepository.findByBusinessIdAndStaffIsNullAndDate(businessId, today);
        if (holidayOpt.isPresent()) {
            return new OpenStatusResponse(false, null, null, "Tatil günü");
        }

        // 4. WorkingHour tablosunu kontrol et: business için staff=null olan ve dayOfWeek=bugünün günü olan kayıt var mı?
        List<WorkingHour> workingHours = workingHourRepository.findByBusinessIdAndStaffIsNull(businessId);
        WorkingHour todayWorkingHour = workingHours.stream()
                .filter(wh -> wh.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(null);

        // - Kayıt yoksa -> open:false, reason:"Çalışma saati tanımlı değil"
        if (todayWorkingHour == null) {
            return new OpenStatusResponse(false, null, null, "Çalışma saati tanımlı değil");
        }

        // - isClosed=true ise -> open:false, reason:"Kapalı gün"
        if (Boolean.TRUE.equals(todayWorkingHour.getIsClosed())) {
            return new OpenStatusResponse(false, null, null, "Kapalı gün");
        }

        LocalTime openTime = todayWorkingHour.getOpenTime();
        LocalTime closeTime = todayWorkingHour.getCloseTime();

        // - Şu anki saat openTime ile closeTime arasında mı?
        if (openTime != null && closeTime != null) {
            if (!currentTime.isBefore(openTime) && currentTime.isBefore(closeTime)) {
                // - Evet -> open:true, opensAt, closesAt dolu, reason: null
                return new OpenStatusResponse(true, openTime, closeTime, null);
            } else {
                // - Hayır -> open:false, reason:"Çalışma saati dışı", opensAt/closesAt yine de doldurulur
                return new OpenStatusResponse(false, openTime, closeTime, "Çalışma saati dışı");
            }
        }

        return new OpenStatusResponse(false, null, null, "Çalışma saati tanımlı değil");
    }
}
