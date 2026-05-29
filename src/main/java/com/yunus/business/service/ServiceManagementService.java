package com.yunus.business.service;

import com.yunus.business.dto.CreateServiceRequest;
import com.yunus.business.dto.ServiceResponse;
import com.yunus.business.dto.UpdateServiceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme hizmetlerinin CRUD ve görsel yönetimi için servis sözleşmesi.
 * "ServiceManagementService" adı; Spring @Service bean'i ve
 * com.yunus.business.entity.Service ile isim çakışmasını önler.
 */
public interface ServiceManagementService {

    /**
     * İşletmenin aktif hizmetlerini sortOrder'a göre sıralı döner.
     */
    List<ServiceResponse> getServices(UUID businessId);

    /**
     * Tek bir hizmetin detayını döner; işletmeye ait değilse hata verir.
     */
    ServiceResponse getService(UUID businessId, UUID serviceId);

    /**
     * Yeni hizmet oluşturur; yalnızca işletme sahibi yapabilir.
     */
    ServiceResponse createService(UUID businessId, CreateServiceRequest request);

    /**
     * Mevcut hizmeti günceller; görsel alanına dokunmaz.
     */
    ServiceResponse updateService(UUID businessId, UUID serviceId, UpdateServiceRequest request);

    /**
     * Hizmet görseli yükler; varsa eskiyi R2'den siler.
     */
    ServiceResponse uploadServiceImage(UUID businessId, UUID serviceId, MultipartFile file);

    /**
     * Hizmet görselini R2'den ve DB'den siler.
     */
    ServiceResponse deleteServiceImage(UUID businessId, UUID serviceId);

    /**
     * Hizmeti pasife alır; kayıt silinmez.
     */
    ServiceResponse deactivateService(UUID businessId, UUID serviceId);

    /**
     * Pasif hizmeti tekrar aktif yapar.
     */
    ServiceResponse activateService(UUID businessId, UUID serviceId);
}
