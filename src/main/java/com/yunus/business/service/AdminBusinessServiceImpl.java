package com.yunus.business.service;

import com.yunus.business.dto.AdminBusinessDetailResponse;
import com.yunus.business.dto.AdminBusinessListResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessStatus;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.exception.ErrorType;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin paneli işletme yönetimi arayüzünün (AdminBusinessService) iş mantığı uygulaması.
 * Durum geçiş kurallarını, validasyon kontrollerini ve veri dönüşümlerini yönetir.
 */
@Service
@Transactional
public class AdminBusinessServiceImpl implements AdminBusinessService {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessServiceImpl.class);

    private final BusinessRepository businessRepository;

    /**
     * İş mantığı sınıfı için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param businessRepository İşletme veri erişim repository nesnesi
     */
    public AdminBusinessServiceImpl(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminBusinessListResponse> getAllBusinesses(BusinessStatus status, Pageable pageable) {
        log.info("Admin list businesses requested with status filter: {}", status);
        if (status == null) {
            return businessRepository.findAll(pageable)
                    .map(b -> b.toAdminListResponse());
        }
        return businessRepository.findByStatus(status, pageable)
                .map(b -> b.toAdminListResponse());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBusinessDetailResponse getBusinessDetail(UUID id) {
        log.info("Admin requested business detail for id: {}", id);
        Business business = findBusinessById(id);
        return business.toAdminDetailResponse();
    }

    @Override
    public void approveBusiness(UUID id) {
        log.info("Admin approved business for id: {}", id);
        Business business = findBusinessById(id);
        
        business.setStatus(BusinessStatus.APPROVED);
        business.setRejectionReason(null);
        businessRepository.save(business);
    }

    @Override
    public void rejectBusiness(UUID id, String reason) {
        log.info("Admin rejected business for id: {} with reason: {}", id, reason);
        validateReason(reason);
        Business business = findBusinessById(id);

        business.setStatus(BusinessStatus.REJECTED);
        business.setRejectionReason(reason.trim());
        businessRepository.save(business);
    }

    @Override
    public void suspendBusiness(UUID id, String reason) {
        log.info("Admin suspended business for id: {} with reason: {}", id, reason);
        validateReason(reason);
        Business business = findBusinessById(id);

        business.setStatus(BusinessStatus.SUSPENDED);
        business.setRejectionReason(reason.trim());
        businessRepository.save(business);
    }

    @Override
    public void activateBusiness(UUID id) {
        log.info("Admin activated business for id: {}", id);
        Business business = findBusinessById(id);

        business.setStatus(BusinessStatus.APPROVED);
        business.setRejectionReason(null);
        businessRepository.save(business);
    }

    /**
     * Kimlik bilgisine göre işletmeyi veri tabanından sorgular.
     * Bulunamaması durumunda ApiException fırlatır.
     *
     * @param id Sorgulanacak işletme kimliği
     * @return Bulunan işletme nesnesi
     * @throws ApiException İşletme bulunamadığında fırlatılır (ErrorType.BUSINESS_NOT_FOUND, 404)
     */
    private Business findBusinessById(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorType.BUSINESS_NOT_FOUND, "İşletme bulunamadı"));
    }

    /**
     * Red ve askıya alma işlemlerinde gerekçe alanının girilmiş olmasını doğrular.
     * Gerekçe boş veya null ise doğrulama hatası fırlatır.
     *
     * @param reason Doğrulanacak gerekçe metni
     * @throws ApiException Gerekçe geçersiz olduğunda fırlatılır (ErrorType.VALIDATION_ERROR, 400)
     */
    private void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(ErrorType.VALIDATION_ERROR, "Sebep (reason) zorunludur");
        }
    }
}
