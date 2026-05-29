package com.yunus.business.service;

import com.yunus.business.dto.CreateStaffRequest;
import com.yunus.business.dto.ServiceResponse;
import com.yunus.business.dto.StaffResponse;
import com.yunus.business.dto.UpdateStaffRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.BusinessException;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.storage.service.StorageService;
import com.yunus.user.entity.User;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme personelinin iş mantığını uygular.
 * Staff entity'deki ManyToMany (services) ilişkisi LAZY yüklenir;
 * tüm metodlar @Transactional altında çalıştığından getServices() çağrısı
 * transaction içinde gerçekleşir — N+1 sorununa dikkat edilmesi için
 * gerektiğinde @EntityGraph veya JOIN FETCH sorgusu eklenebilir.
 */
@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public StaffServiceImpl(BusinessRepository businessRepository,
                            StaffRepository staffRepository,
                            ServiceRepository serviceRepository,
                            StorageService storageService,
                            CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.staffRepository = staffRepository;
        this.serviceRepository = serviceRepository;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    /**
     * İşletmenin aktif personelini sortOrder'a göre sıralı döner.
     * Business bulunamazsa ResourceNotFoundException fırlatılır.
     * ManyToMany (services) LAZY olduğu için toResponse içindeki
     * getServices() çağrısı bu transaction içinde güvenle çalışır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getStaffList(UUID businessId) {
        // İşletmenin var olup olmadığını doğrula
        getBusiness(businessId);
        return staffRepository
                .findByBusinessIdAndIsActiveTrueOrderBySortOrderAsc(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tek bir personelin detayını döner.
     * Personel bu işletmeye ait değilse ResourceNotFoundException fırlatılır
     * (ForbiddenException değil — başka işletmenin personelinin varlığı ifşa edilmez).
     */
    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID businessId, UUID staffId) {
        getBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);
        return toResponse(staff);
    }

    /**
     * Yeni personel oluşturur ve DB'ye kaydeder.
     * profilePhotoUrl başlangıçta null; services başlangıçta boş küme.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public StaffResponse createStaff(UUID businessId, CreateStaffRequest request) {
        // Sahip kontrolü; değilse ForbiddenException
        Business business = getOwnedBusiness(businessId);

        Staff staff = new Staff();
        staff.setBusiness(business);
        staff.setFullName(request.fullName().trim());
        staff.setTitle(request.title());
        staff.setBio(request.bio());
        staff.setSortOrder(request.sortOrder());
        // Fotoğraf ve servisler ayrı endpoint'lerle yönetilir; başlangıçta boş
        staff.setProfilePhotoUrl(null);
        staff.setServices(new HashSet<>());

        Staff saved = staffRepository.save(staff);
        log.info("Personel oluşturuldu: businessId={} staffId={}", businessId, saved.getId());
        return toResponse(saved);
    }

    /**
     * Mevcut personel bilgisini günceller.
     * profilePhotoUrl ve services'e dokunulmaz; değişiklik ayrı endpoint'lerden yapılır.
     * Yalnızca işletme sahibi çağırabilir.
     */
    @Override
    public StaffResponse updateStaff(UUID businessId, UUID staffId, UpdateStaffRequest request) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        Staff staff = getStaffOfBusiness(businessId, staffId);
        staff.setFullName(request.fullName().trim());
        // title ve bio null gönderilirse temizlenir — bilinçli tercih
        staff.setTitle(request.title());
        staff.setBio(request.bio());
        // sortOrder null gelirse mevcut değer korunur
        if (request.sortOrder() != null) {
            staff.setSortOrder(request.sortOrder());
        }

        Staff saved = staffRepository.save(staff);
        log.info("Personel güncellendi: businessId={} staffId={}", businessId, staffId);
        return toResponse(saved);
    }

    /**
     * Personel profil fotoğrafı yükler.
     * Kritik sıra:
     *   1. Eski fotoğraf varsa R2'den sil (yetim dosya kalmaması için)
     *   2. Yeni fotoğrafı R2'ye yükle
     *   3. profilePhotoUrl'i güncelle ve DB'ye kaydet
     * staffId zaten mevcut olduğundan "önce kaydet → sonra yükle" senaryosu gerekmez.
     */
    @Override
    public StaffResponse uploadProfilePhoto(UUID businessId, UUID staffId, MultipartFile file) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        Staff staff = getStaffOfBusiness(businessId, staffId);

        // Eski fotoğraf varsa önce R2'den sil; yetim dosya kalmaması için
        if (staff.getProfilePhotoUrl() != null) {
            storageService.deleteFile(staff.getProfilePhotoUrl());
            log.info("Eski profil fotoğrafı silindi: staffId={}", staffId);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Profil fotoğrafı byte okuma hatası staffId={}", staffId, e);
            throw new BusinessException("Dosya okunamadı");
        }

        // Storage path: staff-photos/{businessId}/{staffId}/{filename}
        String folderName = "staff-photos/" + businessId + "/" + staffId;
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UUID.randomUUID() + ".jpg";

        String url;
        try {
            url = storageService.uploadFile(bytes, originalFileName, folderName);
        } catch (Exception e) {
            log.error("R2 yükleme hatası: businessId={} staffId={} fileName={}",
                    businessId, staffId, originalFileName, e);
            throw new BusinessException("Profil fotoğrafı yüklenemedi");
        }

        staff.setProfilePhotoUrl(url);
        Staff saved = staffRepository.save(staff);
        log.info("Profil fotoğrafı yüklendi: staffId={} url={}", staffId, url);
        return toResponse(saved);
    }

    /**
     * Profil fotoğrafını R2'den siler ve profilePhotoUrl'i null yapar.
     * profilePhotoUrl zaten null ise işlem yapılmaz — idempotent davranış.
     */
    @Override
    public StaffResponse deleteProfilePhoto(UUID businessId, UUID staffId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        Staff staff = getStaffOfBusiness(businessId, staffId);

        // Fotoğraf yoksa işlem yapma; idempotent — çift çağrıda hata vermez
        if (staff.getProfilePhotoUrl() == null) {
            log.info("Silinecek profil fotoğrafı yok: staffId={}", staffId);
            return toResponse(staff);
        }

        // Önce R2'den sil; sonra DB güncelle
        storageService.deleteFile(staff.getProfilePhotoUrl());
        staff.setProfilePhotoUrl(null);
        Staff saved = staffRepository.save(staff);
        log.info("Profil fotoğrafı silindi: staffId={}", staffId);
        return toResponse(saved);
    }

    /**
     * Personeli pasife alır; fiziksel silme yapmaz.
     * Pasif personel getStaffList'te görünmez; geçmiş randevuları etkilenmez.
     */
    @Override
    public StaffResponse deactivateStaff(UUID businessId, UUID staffId) {
        getOwnedBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);
        staff.setIsActive(false);
        Staff saved = staffRepository.save(staff);
        log.info("Personel pasife alındı: businessId={} staffId={}", businessId, staffId);
        return toResponse(saved);
    }

    /**
     * Pasif personeli tekrar aktive eder.
     */
    @Override
    public StaffResponse activateStaff(UUID businessId, UUID staffId) {
        getOwnedBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);
        staff.setIsActive(true);
        Staff saved = staffRepository.save(staff);
        log.info("Personel aktive edildi: businessId={} staffId={}", businessId, staffId);
        return toResponse(saved);
    }

    /**
     * Personele hizmet atar.
     * Her serviceId için:
     *   1. Hizmet DB'de var mı?
     *   2. Bu işletmeye ait mi? (başka işletmenin servisi atanamaz)
     * Mevcut servisler korunur; sadece yeniler eklenir (addAll).
     */
    @Override
    public StaffResponse assignServices(UUID businessId, UUID staffId, List<UUID> serviceIds) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);

        // Her serviceId'yi doğrula; bulunamazsa veya başka işletmeyse hata ver
        Set<com.yunus.business.entity.Service> newServices = resolveServicesOfBusiness(businessId, serviceIds);

        // Mevcut servislere ekle; var olanlar tekrar eklense de Set garantisi sağlar
        staff.getServices().addAll(newServices);
        Staff saved = staffRepository.save(staff);
        log.info("Personele {} hizmet atandı: businessId={} staffId={}", newServices.size(), businessId, staffId);
        return toResponse(saved);
    }

    /**
     * Personelden belirtilen hizmetleri kaldırır.
     * Listede olmayan bir serviceId gönderilirse sessizce yok sayılır;
     * removeAll Set semantiği gereği hata vermez.
     */
    @Override
    public StaffResponse removeServices(UUID businessId, UUID staffId, List<UUID> serviceIds) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);
        Staff staff = getStaffOfBusiness(businessId, staffId);

        // Kaldırılacak servisleri ID üzerinden bul ve çıkar
        Set<com.yunus.business.entity.Service> toRemove = resolveServicesOfBusiness(businessId, serviceIds);
        staff.getServices().removeAll(toRemove);
        Staff saved = staffRepository.save(staff);
        log.info("Personelden {} hizmet kaldırıldı: businessId={} staffId={}", toRemove.size(), businessId, staffId);
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
     * Sahip değilse ForbiddenException — başka işletmelerin verisi sızmaz.
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
     * Personeli ID ile getirir ve bu işletmeye ait olduğunu doğrular.
     * Ait değilse ResourceNotFoundException (ForbiddenException değil) —
     * başka işletmenin personelinin varlığı ifşa edilmez.
     */
    private Staff getStaffOfBusiness(UUID businessId, UUID staffId) {
        return staffRepository.findById(staffId)
                .filter(s -> s.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Personel", "id", staffId));
    }

    /**
     * Verilen serviceId listesini bu işletmeye ait hizmetlere çözer.
     * Bulunamazsa veya başka işletmeye aitse ResourceNotFoundException fırlatılır.
     * Çapraz işletme hizmet atama saldırılarını önler.
     */
    private Set<com.yunus.business.entity.Service> resolveServicesOfBusiness(
            UUID businessId, List<UUID> serviceIds) {
        Set<com.yunus.business.entity.Service> result = new HashSet<>();
        for (UUID serviceId : serviceIds) {
            com.yunus.business.entity.Service service = serviceRepository.findById(serviceId)
                    // Bu işletmeye ait mi kontrol et
                    .filter(s -> s.getBusiness().getId().equals(businessId))
                    .orElseThrow(() -> new ResourceNotFoundException("Hizmet", "id", serviceId));
            result.add(service);
        }
        return result;
    }

    /**
     * Staff entity'sini StaffResponse DTO'suna dönüştürür.
     * Entity hiçbir zaman doğrudan controller'a çıkmaz.
     * getServices() çağrısı LAZY ilişki için transaction içinde olması şart;
     * tüm public metodlar @Transactional ile korunduğundan bu güvende.
     */
    private StaffResponse toResponse(Staff staff) {
        // ManyToMany ilişkisini ServiceResponse listesine dönüştür
        List<ServiceResponse> serviceResponses = staff.getServices().stream()
                .map(s -> new ServiceResponse(
                        s.getId(),
                        s.getBusiness().getId(),
                        s.getName(),
                        s.getDescription(),
                        s.getDurationMin(),
                        s.getPrice(),
                        s.getCurrency(),
                        s.getImageUrl(),
                        s.getIsActive(),
                        s.getSortOrder(),
                        s.getCreatedAt(),
                        s.getUpdatedAt()
                ))
                .toList();

        return new StaffResponse(
                staff.getId(),
                staff.getBusiness().getId(),
                staff.getFullName(),
                staff.getTitle(),
                staff.getBio(),
                staff.getProfilePhotoUrl(),
                staff.getIsActive(),
                staff.getSortOrder(),
                serviceResponses,
                staff.getCreatedAt(),
                staff.getUpdatedAt()
        );
    }
}
