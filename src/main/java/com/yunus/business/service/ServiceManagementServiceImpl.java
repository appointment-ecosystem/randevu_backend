package com.yunus.business.service;

import com.yunus.business.dto.CreateServiceRequest;
import com.yunus.business.dto.ServiceResponse;
import com.yunus.business.dto.UpdateServiceRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.storage.service.StorageService;
import com.yunus.user.entity.User;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme hizmetlerinin iş mantığını uygular.
 * Görsel yükleme işleminde "önce kaydet → sonra yükle → sonra güncelle" sırası
 * zorunludur; serviceId yükleme path'inde kullanılır, henüz persist olmadan bilinmez.
 */
@Service
@Transactional
public class ServiceManagementServiceImpl implements ServiceManagementService {

    private static final Logger log = LoggerFactory.getLogger(ServiceManagementServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public ServiceManagementServiceImpl(BusinessRepository businessRepository,
                                        ServiceRepository serviceRepository,
                                        StorageService storageService,
                                        CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.serviceRepository = serviceRepository;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    /**
     * İşletmenin aktif hizmetlerini sortOrder'a göre sıralı döner.
     * Business bulunamazsa ResourceNotFoundException fırlatılır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServices(UUID businessId) {
        // İşletmenin var olup olmadığını doğrula
        getBusiness(businessId);
        return serviceRepository
                .findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tek bir hizmetin detayını döner.
     * Hizmet bu işletmeye ait değilse ResourceNotFoundException fırlatılır
     * (ForbiddenException değil — başka işletmenin servis ID'si varmış gibi görünmesini önler).
     */
    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getService(UUID businessId, UUID serviceId) {
        getBusiness(businessId);
        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);
        return toResponse(service);
    }

    /**
     * Yeni hizmet oluşturur ve DB'ye kaydeder.
     * imageUrl başlangıçta null; görsel sonradan uploadServiceImage ile eklenir.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public ServiceResponse createService(UUID businessId, CreateServiceRequest request) {
        // Sahip kontrolü; değilse ForbiddenException
        Business business = getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = new com.yunus.business.entity.Service();
        service.setBusiness(business);
        service.setName(request.name().trim());
        service.setDescription(request.description());
        service.setDurationMin(request.durationMin());
        service.setPrice(request.price());
        service.setCurrency(request.currency());
        service.setSortOrder(request.sortOrder());
        // imageUrl başlangıçta null — ayrı endpoint ile yüklenir
        service.setImageUrl(null);

        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet oluşturuldu: businessId={} serviceId={}", businessId, saved.getId());
        return toResponse(saved);
    }

    /**
     * Mevcut hizmetin alanlarını günceller.
     * imageUrl kasıtlı olarak dokunulmaz; görsel yönetimi ayrı metoddadır.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public ServiceResponse updateService(UUID businessId, UUID serviceId, UpdateServiceRequest request) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);
        service.setName(request.name().trim());
        service.setDescription(request.description());
        service.setDurationMin(request.durationMin());
        service.setPrice(request.price());

        // Para birimi null gelirse mevcut değer korunur
        if (request.currency() != null && !request.currency().isBlank()) {
            service.setCurrency(request.currency());
        }
        // Sıralama null gelirse mevcut değer korunur
        if (request.sortOrder() != null) {
            service.setSortOrder(request.sortOrder());
        }

        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet güncellendi: businessId={} serviceId={}", businessId, serviceId);
        return toResponse(saved);
    }

    /**
     * Hizmet görseli yükler.
     * Kritik sıra:
     *   1. Mevcut görseli sil (varsa)
     *   2. R2'ye yeni görseli yükle
     *   3. imageUrl'i güncelle ve kaydet
     * "Önce kaydet, sonra yükle" burada GEREKMEZ; serviceId zaten mevcut.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public ServiceResponse uploadServiceImage(UUID businessId, UUID serviceId, MultipartFile file) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);

        // Eski görsel varsa önce R2'den sil; yetim dosya kalmaması için
        if (service.getImageUrl() != null) {
            storageService.deleteFile(service.getImageUrl());
            log.info("Eski hizmet görseli silindi: serviceId={}", serviceId);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Hizmet görseli byte okuma hatası serviceId={}", serviceId, e);
            throw new BusinessException("Dosya okunamadı");
        }

        // Storage path: service-images/{businessId}/{serviceId}/{filename}
        String folderName = "service-images/" + businessId + "/" + serviceId;
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UUID.randomUUID() + ".jpg";

        String url;
        try {
            url = storageService.uploadFile(bytes, originalFileName, folderName);
        } catch (Exception e) {
            log.error("R2 yükleme hatası: businessId={} serviceId={} fileName={}",
                    businessId, serviceId, originalFileName, e);
            throw new BusinessException("Hizmet görseli yüklenemedi");
        }

        service.setImageUrl(url);
        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet görseli yüklendi: serviceId={} url={}", serviceId, url);
        return toResponse(saved);
    }

    /**
     * Hizmet görselini R2'den siler ve imageUrl'i null yapar.
     * imageUrl zaten null ise işlem yapılmaz; idempotent davranış sağlanır.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public ServiceResponse deleteServiceImage(UUID businessId, UUID serviceId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);

        // Görsel yoksa işlem yapma; idempotent — çift çağrıda hata vermez
        if (service.getImageUrl() == null) {
            log.info("Silinecek görsel yok: serviceId={}", serviceId);
            return toResponse(service);
        }

        // Önce R2'den sil; DB güncellemesi sonrasında yapılırsa yetim dosya kalabilir
        storageService.deleteFile(service.getImageUrl());
        service.setImageUrl(null);
        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet görseli silindi: serviceId={}", serviceId);
        return toResponse(saved);
    }

    /**
     * Hizmeti pasife alır; fiziksel silme yapmaz.
     * Pasif hizmetler getServices listesinde görünmez;
     * mevcut randevular ise bundan etkilenmez.
     */
    @Override
    public ServiceResponse deactivateService(UUID businessId, UUID serviceId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);
        service.setIsActive(false);
        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet pasife alındı: businessId={} serviceId={}", businessId, serviceId);
        return toResponse(saved);
    }

    /**
     * Pasif hizmeti tekrar aktive eder.
     * Daha önce pasife alınan hizmetlerin geri açılması için kullanılır.
     */
    @Override
    public ServiceResponse activateService(UUID businessId, UUID serviceId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        com.yunus.business.entity.Service service = getServiceOfBusiness(businessId, serviceId);
        service.setIsActive(true);
        com.yunus.business.entity.Service saved = serviceRepository.save(service);
        log.info("Hizmet aktive edildi: businessId={} serviceId={}", businessId, serviceId);
        return toResponse(saved);
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
     * Sahip değilse ForbiddenException — kimlik bilgisi sızmaz.
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
     * Hizmeti ID ile getirir ve bu işletmeye ait olduğunu doğrular.
     * Ait değilse ResourceNotFoundException — başkasının hizmet ID'si varsa
     * ForbiddenException yerine NOT_FOUND semantiği tercih edilir.
     */
    private com.yunus.business.entity.Service getServiceOfBusiness(UUID businessId, UUID serviceId) {
        return serviceRepository.findById(serviceId)
                .filter(s -> s.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Hizmet", "id", serviceId));
    }

    /**
     * Service entity'sini ServiceResponse DTO'suna dönüştürür.
     * Entity hiçbir zaman doğrudan controller'a çıkmaz.
     */
    private ServiceResponse toResponse(com.yunus.business.entity.Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getBusiness().getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMin(),
                service.getPrice(),
                service.getCurrency(),
                service.getImageUrl(),
                service.getIsActive(),
                service.getSortOrder(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
