package com.yunus.business.service;

import com.yunus.business.dto.BulkWorkingHourRequest;
import com.yunus.business.dto.WorkingHourRequest;
import com.yunus.business.dto.WorkingHourResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.entity.WorkingHour;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.business.repository.WorkingHourRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çalışma saati iş mantığını uygular.
 * "Önce sil sonra yeniden oluştur" stratejisi ile 7 günlük tam set garantilenir;
 * upsert mantığı UNIQUE constraint çakışmalarına yol açabileceğinden tercih edilmez.
 *
 * Önemli: WorkingHour entity'de openTime/closeTime nullable=false olduğundan,
 * isClosed=true olan günler için dummy değer (LocalTime.MIDNIGHT) atanır.
 * Bu sayede DB kısıtı ihlal edilmez; isClosed bayrağı gerçek değeri taşır.
 */
@Service
@Transactional
public class WorkingHourServiceImpl implements WorkingHourService {

    private static final Logger log = LoggerFactory.getLogger(WorkingHourServiceImpl.class);

    // isClosed=true günlerde openTime/closeTime için atanan placeholder değer
    private static final LocalTime CLOSED_DAY_PLACEHOLDER = LocalTime.MIDNIGHT;

    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;
    private final WorkingHourRepository workingHourRepository;
    private final CurrentUserService currentUserService;

    public WorkingHourServiceImpl(BusinessRepository businessRepository,
                                  StaffRepository staffRepository,
                                  WorkingHourRepository workingHourRepository,
                                  CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.staffRepository = staffRepository;
        this.workingHourRepository = workingHourRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * İşletmenin genel çalışma saatlerini dayOfWeek'e göre sıralı döner.
     * Business bulunamazsa ResourceNotFoundException fırlatılır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkingHourResponse> getBusinessHours(UUID businessId) {
        Business business = getBusiness(businessId);
        return workingHourRepository
                .findByBusinessAndStaffIsNullOrderByDayOfWeekAsc(business)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Belirtilen personelin çalışma saatlerini dayOfWeek'e göre sıralı döner.
     * Staff bu business'a ait değilse ResourceNotFoundException fırlatılır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkingHourResponse> getStaffHours(UUID businessId, UUID staffId) {
        Business business = getBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);
        return workingHourRepository
                .findByBusinessAndStaffOrderByDayOfWeekAsc(business, staff)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * İşletme geneli çalışma saatlerini tamamen yeniden set eder.
     * Strateji: mevcut kayıtlar silinir → yeni kayıtlar oluşturulur.
     * Bu yaklaşım UNIQUE constraint çakışmalarını önler ve tam tutarlılık sağlar.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public List<WorkingHourResponse> setBusinessHours(UUID businessId, BulkWorkingHourRequest request) {
        Business business = getOwnedBusiness(businessId);

        // Mevcut işletme geneli saatleri sil (staff_id IS NULL olanlar)
        List<WorkingHour> existing = workingHourRepository
                .findByBusinessAndStaffIsNullOrderByDayOfWeekAsc(business);
        workingHourRepository.deleteAll(existing);

        // Yeni çalışma saatlerini oluştur ve kaydet
        List<WorkingHour> newHours = request.hours().stream()
                .map(req -> buildWorkingHour(req, business, null))
                .toList();

        List<WorkingHour> saved = workingHourRepository.saveAll(newHours);
        log.info("İşletme saatleri güncellendi: businessId={} count={}", businessId, saved.size());

        return saved.stream().map(this::toResponse).toList();
    }

    /**
     * Personel çalışma saatlerini tamamen yeniden set eder.
     * Mevcut saatler silinir; yenileri oluşturulur.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public List<WorkingHourResponse> setStaffHours(UUID businessId, UUID staffId,
                                                    BulkWorkingHourRequest request) {
        Business business = getOwnedBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);

        // Mevcut personel saatlerini sil
        List<WorkingHour> existing = workingHourRepository
                .findByBusinessAndStaffOrderByDayOfWeekAsc(business, staff);
        workingHourRepository.deleteAll(existing);

        // Yeni çalışma saatlerini oluştur
        List<WorkingHour> newHours = request.hours().stream()
                .map(req -> buildWorkingHour(req, business, staff))
                .toList();

        List<WorkingHour> saved = workingHourRepository.saveAll(newHours);
        log.info("Personel saatleri güncellendi: businessId={} staffId={} count={}",
                businessId, staffId, saved.size());

        return saved.stream().map(this::toResponse).toList();
    }

    /**
     * Tek bir günün çalışma saatini günceller.
     * WorkingHour bu business'a ait değilse ForbiddenException fırlatılır.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public WorkingHourResponse updateOneDay(UUID businessId, UUID workingHourId, WorkingHourRequest request) {
        getOwnedBusiness(businessId);

        WorkingHour wh = workingHourRepository.findById(workingHourId)
                .orElseThrow(() -> new ResourceNotFoundException("Çalışma saati", "id", workingHourId));

        // Bu business'a ait mi kontrol et
        if (!wh.getBusiness().getId().equals(businessId)) {
            throw new ForbiddenException("Bu çalışma saati size ait değil");
        }

        // isClosed=false ise openTime ve closeTime zorunludur
        validateTimeFields(request);

        wh.setDayOfWeek(request.dayOfWeek());
        wh.setIsClosed(request.isClosed());

        // isClosed=true ise placeholder, false ise gerçek değer atanır
        if (Boolean.TRUE.equals(request.isClosed())) {
            wh.setOpenTime(CLOSED_DAY_PLACEHOLDER);
            wh.setCloseTime(CLOSED_DAY_PLACEHOLDER);
        } else {
            wh.setOpenTime(request.openTime());
            wh.setCloseTime(request.closeTime());
        }

        WorkingHour saved = workingHourRepository.save(wh);
        log.info("Çalışma saati güncellendi: workingHourId={}", workingHourId);
        return toResponse(saved);
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    /**
     * WorkingHourRequest'ten WorkingHour entity oluşturur.
     * isClosed=false ise openTime/closeTime zorunluluğu burada validate edilir.
     * isClosed=true günler için placeholder değer atanır (entity nullable=false kısıtı).
     */
    private WorkingHour buildWorkingHour(WorkingHourRequest req, Business business, Staff staff) {
        // Açık günlerde saat alanları zorunludur
        validateTimeFields(req);

        WorkingHour wh = new WorkingHour();
        wh.setBusiness(business);
        wh.setStaff(staff);
        wh.setDayOfWeek(req.dayOfWeek());
        wh.setIsClosed(req.isClosed());

        // isClosed=true ise DB nullable=false kısıtını karşılamak için placeholder;
        // client isClosed bayrağından gerçek durumu okur, bu değerleri yok sayar
        if (Boolean.TRUE.equals(req.isClosed())) {
            wh.setOpenTime(CLOSED_DAY_PLACEHOLDER);
            wh.setCloseTime(CLOSED_DAY_PLACEHOLDER);
        } else {
            wh.setOpenTime(req.openTime());
            wh.setCloseTime(req.closeTime());
        }

        return wh;
    }

    /**
     * isClosed=false iken openTime veya closeTime null ise BusinessException fırlatır.
     * Bu kontrol entity'nin DB kısıtından önce anlamlı hata mesajı üretmek için gereklidir.
     */
    private void validateTimeFields(WorkingHourRequest req) {
        if (Boolean.FALSE.equals(req.isClosed())) {
            if (req.openTime() == null || req.closeTime() == null) {
                throw new BusinessException(
                        "Açık gün için açılış ve kapanış saati zorunludur (gün: " + req.dayOfWeek() + ")");
            }
        }
    }

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
     * WorkingHour entity'sini WorkingHourResponse DTO'suna dönüştürür.
     * isClosed=true olan günlerde openTime/closeTime placeholder içerir;
     * response'ta olduğu gibi döner — client isClosed bayrağına göre yorum yapar.
     */
    private WorkingHourResponse toResponse(WorkingHour wh) {
        return new WorkingHourResponse(
                wh.getId(),
                wh.getBusiness().getId(),
                wh.getStaff() != null ? wh.getStaff().getId() : null,
                wh.getDayOfWeek(),
                // isClosed=true ise placeholder dönebilir; client isClosed bayrağına bakmalı
                Boolean.TRUE.equals(wh.getIsClosed()) ? null : wh.getOpenTime(),
                Boolean.TRUE.equals(wh.getIsClosed()) ? null : wh.getCloseTime(),
                wh.getIsClosed()
        );
    }
}
