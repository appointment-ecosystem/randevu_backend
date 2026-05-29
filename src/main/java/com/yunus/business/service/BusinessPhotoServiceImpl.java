package com.yunus.business.service;

import com.yunus.business.dto.BusinessPhotoResponse;
import com.yunus.business.dto.PhotoSortRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessPhoto;
import com.yunus.business.repository.BusinessPhotoRepository;
import com.yunus.business.repository.BusinessRepository;
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
 * İşletme fotoğraflarının yükleme, silme, sıralama ve kapak
 * belirleme mantığını uygular. R2 ve DB arasındaki koordinasyonu sağlar.
 */
@Service
@Transactional
public class BusinessPhotoServiceImpl implements BusinessPhotoService {

    private static final Logger log = LoggerFactory.getLogger(BusinessPhotoServiceImpl.class);

    private final BusinessRepository businessRepository;
    private final BusinessPhotoRepository businessPhotoRepository;
    private final StorageService storageService;
    private final CurrentUserService currentUserService;

    public BusinessPhotoServiceImpl(BusinessRepository businessRepository,
                                    BusinessPhotoRepository businessPhotoRepository,
                                    StorageService storageService,
                                    CurrentUserService currentUserService) {
        this.businessRepository = businessRepository;
        this.businessPhotoRepository = businessPhotoRepository;
        this.storageService = storageService;
        this.currentUserService = currentUserService;
    }

    /**
     * İşletmenin tüm fotoğraflarını sortOrder'a göre sıralı döner.
     * İşletme bulunamazsa ResourceNotFoundException fırlatılır.
     */
    @Override
    @Transactional(readOnly = true)
    public List<BusinessPhotoResponse> getPhotos(UUID businessId) {
        // İşletmenin var olup olmadığını kontrol et
        getBusiness(businessId);
        return businessPhotoRepository
                .findByBusinessIdOrderBySortOrderAsc(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Dosyayı R2'ye yükler, DB kaydı oluşturur ve yanıt döner.
     * Yalnızca işletme sahibi çağırabilir; yükleme başarısız olursa BusinessException fırlatılır.
     */
    @Override
    public BusinessPhotoResponse uploadPhoto(UUID businessId, MultipartFile file) {
        // Sahip kontrolü: yalnızca bu işletmenin sahibi fotoğraf ekleyebilir
        Business business = getOwnedBusiness(businessId);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Fotoğraf byte okuma hatası businessId={}", businessId, e);
            throw new BusinessException("Dosya okunamadı");
        }

        // R2'ye yükleme; hata olursa BusinessException ile sarılır
        String folderName = "business-photos/" + businessId;
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : UUID.randomUUID() + ".jpg";

        String url;
        try {
            url = storageService.uploadFile(bytes, originalFileName, folderName);
        } catch (Exception e) {
            log.error("R2 yükleme hatası businessId={} fileName={}", businessId, originalFileName, e);
            throw new BusinessException("Fotoğraf yüklenemedi");
        }

        // DB kaydı oluştur; sortOrder varsayılan 0 — client daha sonra updateSortOrder ile düzenleyebilir
        BusinessPhoto photo = new BusinessPhoto();
        photo.setBusiness(business);
        photo.setUrl(url);
        photo.setFileName(originalFileName);
        photo.setFileSize((int) file.getSize());
        photo.setMimeType(file.getContentType());
        photo.setSortOrder(0);

        BusinessPhoto saved = businessPhotoRepository.save(photo);
        log.info("Fotoğraf yüklendi: businessId={} photoId={}", businessId, saved.getId());
        return toResponse(saved);
    }

    /**
     * Belirtilen fotoğrafı kapak görseli yapar.
     * Bu işletmenin tüm fotoğraflarının isCover değeri sıfırlanır,
     * ardından yalnızca hedef fotoğrafın isCover = true yapılır.
     * Tek kapak kuralı böylece zorunlu kılınır.
     */
    @Override
    public BusinessPhotoResponse setCover(UUID businessId, UUID photoId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        // Bu işletmenin tüm fotoğraflarını getir
        List<BusinessPhoto> allPhotos = businessPhotoRepository
                .findByBusinessIdOrderBySortOrderAsc(businessId);

        // Tüm fotoğrafların kapak bayrağını kaldır
        allPhotos.forEach(p -> p.setIsCover(false));

        // Hedef fotoğrafı bul ve kapak yap
        BusinessPhoto target = allPhotos.stream()
                .filter(p -> p.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Fotoğraf", "id", photoId));
        target.setIsCover(true);

        // Toplu kaydet
        businessPhotoRepository.saveAll(allPhotos);
        log.info("Kapak güncellendi: businessId={} photoId={}", businessId, photoId);
        return toResponse(target);
    }

    /**
     * Fotoğrafı önce R2'den sonra DB'den siler.
     * Sıra önemlidir: DB silinirse R2'de yetim dosya kalabilir;
     * önce R2 silme tercih edilir.
     */
    @Override
    public void deletePhoto(UUID businessId, UUID photoId) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        // Fotoğrafı bul; bu işletmeye ait olmayan ID için ResourceNotFoundException
        BusinessPhoto photo = businessPhotoRepository.findById(photoId)
                .filter(p -> p.getBusiness().getId().equals(businessId))
                .orElseThrow(() -> new ResourceNotFoundException("Fotoğraf", "id", photoId));

        // Önce R2'den sil; sonra DB'den sil
        storageService.deleteFile(photo.getUrl());
        businessPhotoRepository.delete(photo);
        log.info("Fotoğraf silindi: businessId={} photoId={}", businessId, photoId);
    }

    /**
     * Toplu sıralama güncellemesi; her fotoğrafın sortOrder değeri değişir.
     * saveAll tek bir transaction'da çalışır; kısmi başarısızlık olmaz.
     */
    @Override
    public List<BusinessPhotoResponse> updateSortOrder(UUID businessId, List<PhotoSortRequest> requests) {
        // Sahip kontrolü
        getOwnedBusiness(businessId);

        // Her istek için fotoğrafı bul ve sıralamasını güncelle
        List<BusinessPhoto> toUpdate = requests.stream()
                .map(req -> {
                    BusinessPhoto photo = businessPhotoRepository.findById(req.photoId())
                            .filter(p -> p.getBusiness().getId().equals(businessId))
                            .orElseThrow(() -> new ResourceNotFoundException("Fotoğraf", "id", req.photoId()));
                    photo.setSortOrder(req.sortOrder());
                    return photo;
                })
                .toList();

        // Toplu kaydet
        List<BusinessPhoto> saved = businessPhotoRepository.saveAll(toUpdate);
        log.info("Sıralama güncellendi: businessId={} count={}", businessId, saved.size());

        // Güncel listeyi sortOrder'a göre sıralı döndür
        return businessPhotoRepository
                .findByBusinessIdOrderBySortOrderAsc(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Yardımcı metodlar ───────────────────────────────────────────────────

    /**
     * İşletmeyi ID'ye göre getirir; bulunamazsa ResourceNotFoundException fırlatır.
     */
    private Business getBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("İşletme", "id", businessId));
    }

    /**
     * İşletmeyi getirir ve giriş yapmış kullanıcının sahibi olup olmadığını kontrol eder.
     * Sahip değilse ForbiddenException fırlatılır.
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
     * BusinessPhoto entity'sini BusinessPhotoResponse DTO'suna dönüştürür.
     * Entity hiçbir zaman controller'a çıkmaz.
     */
    private BusinessPhotoResponse toResponse(BusinessPhoto photo) {
        return new BusinessPhotoResponse(
                photo.getId(),
                photo.getUrl(),
                photo.getFileName(),
                photo.getFileSize(),
                photo.getMimeType(),
                photo.getIsCover(),
                photo.getSortOrder(),
                photo.getCreatedAt()
        );
    }
}
