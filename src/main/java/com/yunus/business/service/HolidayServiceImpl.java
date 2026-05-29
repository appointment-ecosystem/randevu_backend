package com.yunus.business.service;

import com.yunus.business.dto.HolidayRequest;
import com.yunus.business.dto.HolidayResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Holiday;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.HolidayRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tatil / kapalı gün iş mantığını uygular.
 * staffId null ile işletme geneli, dolu ile personele özel tatil yönetilir.
 */
@Service
@Transactional
public class HolidayServiceImpl implements HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;
    private final HolidayRepository holidayRepository;
    private final CurrentUserService currentUserService;

    public HolidayServiceImpl(BusinessRepository businessRepository,
                               StaffRepository staffRepository,
                               HolidayRepository holidayRepository,
                               CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.staffRepository = staffRepository;
        this.holidayRepository = holidayRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Tatilleri listeler.
     * staffId null ise tüm işletme kapalı günleri, dolu ise personele özel tatiller.
     * date'e göre sıralı döner.
     */
    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getHolidays(UUID businessId, UUID staffId) {
        Business business = getBusiness(businessId);

        if (staffId == null) {
            // İşletme geneli tatiller — staff_id IS NULL olan kayıtlar
            return holidayRepository
                    .findByBusinessAndStaffIsNullOrderByDateAsc(business)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        } else {
            // Personele özel tatiller
            Staff staff = getStaffOfBusiness(businessId, staffId);
            return holidayRepository
                    .findByBusinessAndStaffOrderByDateAsc(business, staff)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
    }

    /**
     * Yeni tatil ekler.
     * Çakışma kontrolü: aynı business + aynı staff + aynı tarih için tatil zaten varsa
     * BusinessException fırlatılır — slot hesaplama tutarlılığı için zorunludur.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public HolidayResponse addHoliday(UUID businessId, HolidayRequest request) {
        Business business = getOwnedBusiness(businessId);

        // staffId dolu ise personeli bul ve bu business'a ait mi kontrol et
        Staff staff = null;
        if (request.staffId() != null) {
            staff = getStaffOfBusiness(businessId, request.staffId());
        }

        // Aynı gün + aynı kapsam (business + staff) için çakışma kontrolü
        boolean alreadyExists = holidayRepository.existsByBusinessAndStaffAndDate(
                business, staff, request.date());
        if (alreadyExists) {
            throw new BusinessException(
                    "Bu tarih için zaten bir tatil kaydı mevcut: " + request.date());
        }

        Holiday holiday = new Holiday();
        holiday.setBusiness(business);
        holiday.setStaff(staff);
        holiday.setDate(request.date());
        holiday.setReason(request.reason());

        Holiday saved = holidayRepository.save(holiday);
        log.info("Tatil eklendi: businessId={} staffId={} date={}", businessId, request.staffId(), request.date());
        return toResponse(saved);
    }

    /**
     * Tatil kaydını siler.
     * Holiday bu business'a ait değilse ForbiddenException fırlatılır.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public void deleteHoliday(UUID businessId, UUID holidayId) {
        getOwnedBusiness(businessId);

        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Tatil", "id", holidayId));

        // Bu business'a ait mi kontrol et
        if (!holiday.getBusiness().getId().equals(businessId)) {
            throw new ForbiddenException("Bu tatil kaydı size ait değil");
        }

        holidayRepository.delete(holiday);
        log.info("Tatil silindi: businessId={} holidayId={}", businessId, holidayId);
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    /**
     * İşletmeyi ID ile getirir; bulunamazsa ResourceNotFoundException fırlatır.
     */
    private Business getBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));
    }

    /**
     * İşletmeyi getirir ve giriş yapan kullanıcının sahibi olduğunu doğrular.
     */
    private Business getOwnedBusiness(UUID businessId) {
        Business business = getBusiness(businessId);
        User currentUser = currentUserService.getCurrentUser();
        if (!business.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bu işletme size ait değil");
        }
        return business;
    }

    /**
     * Personeli bul ve bu işletmeye ait olduğunu doğrula.
     * Ait değilse ResourceNotFoundException — varlık ifşa edilmez.
     */
    private Staff getStaffOfBusiness(UUID businessId, UUID staffId) {
        return staffRepository.findById(staffId)
                .filter(s -> s.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Personel", "id", staffId));
    }

    /**
     * Holiday entity'sini HolidayResponse DTO'suna dönüştürür.
     * Entity controller'a doğrudan çıkmaz.
     */
    private HolidayResponse toResponse(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getBusiness().getId(),
                holiday.getStaff() != null ? holiday.getStaff().getId() : null,
                holiday.getDate(),
                holiday.getReason()
        );
    }
}
