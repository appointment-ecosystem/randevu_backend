package com.yunus.business.service;

import com.yunus.business.dto.CreateStaffRequest;
import com.yunus.business.dto.StaffResponse;
import com.yunus.business.dto.UpdateStaffRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * İşletme personelinin CRUD, fotoğraf ve hizmet atama operasyonları için servis sözleşmesi.
 */
public interface StaffService {

    /**
     * İşletmenin aktif personelini sortOrder'a göre sıralı döner.
     */
    List<StaffResponse> getStaffList(UUID businessId);

    /**
     * Tek bir personelin detayını döner; işletmeye ait değilse hata verir.
     */
    StaffResponse getStaff(UUID businessId, UUID staffId);

    /**
     * Yeni personel oluşturur; yalnızca işletme sahibi yapabilir.
     */
    StaffResponse createStaff(UUID businessId, CreateStaffRequest request);

    /**
     * Mevcut personel bilgisini günceller; fotoğraf ve servislere dokunmaz.
     */
    StaffResponse updateStaff(UUID businessId, UUID staffId, UpdateStaffRequest request);

    /**
     * Personel profil fotoğrafı yükler; varsa eskisini R2'den siler.
     */
    StaffResponse uploadProfilePhoto(UUID businessId, UUID staffId, MultipartFile file);

    /**
     * Profil fotoğrafını R2'den ve DB'den siler.
     */
    StaffResponse deleteProfilePhoto(UUID businessId, UUID staffId);

    /**
     * Personeli pasife alır; fiziksel silme yapmaz.
     */
    StaffResponse deactivateStaff(UUID businessId, UUID staffId);

    /**
     * Pasif personeli tekrar aktive eder.
     */
    StaffResponse activateStaff(UUID businessId, UUID staffId);

    /**
     * Personele hizmet atar; yalnızca bu işletmenin hizmetleri atanabilir.
     */
    StaffResponse assignServices(UUID businessId, UUID staffId, List<UUID> serviceIds);

    /**
     * Personelden belirtilen hizmetleri kaldırır.
     */
    StaffResponse removeServices(UUID businessId, UUID staffId, List<UUID> serviceIds);
}
