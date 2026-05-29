package com.yunus.business.service;

import com.yunus.business.dto.BusinessPhotoResponse;
import com.yunus.business.dto.PhotoSortRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme fotoğraf yönetimi için servis sözleşmesi.
 * Yükleme, silme, kapak belirleme ve sıralama operasyonlarını tanımlar.
 */
public interface BusinessPhotoService {

    /**
     * İşletmeye ait tüm fotoğrafları sortOrder'a göre sıralı döner.
     */
    List<BusinessPhotoResponse> getPhotos(UUID businessId);

    /**
     * Dosyayı R2'ye yükler ve DB kaydını oluşturur.
     */
    BusinessPhotoResponse uploadPhoto(UUID businessId, MultipartFile file);

    /**
     * Belirtilen fotoğrafı kapak görseli yapar; önceki kapağı kaldırır.
     */
    BusinessPhotoResponse setCover(UUID businessId, UUID photoId);

    /**
     * Fotoğrafı R2'den ve DB'den siler.
     */
    void deletePhoto(UUID businessId, UUID photoId);

    /**
     * Birden fazla fotoğrafın sıralama değerini toplu günceller.
     */
    List<BusinessPhotoResponse> updateSortOrder(UUID businessId, List<PhotoSortRequest> requests);
}
