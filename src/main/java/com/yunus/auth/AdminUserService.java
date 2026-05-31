package com.yunus.auth;

import com.yunus.auth.dto.AdminUserAppointmentResponse;
import com.yunus.auth.dto.AdminUserDetailResponse;
import com.yunus.auth.dto.UserRoleUpdateRequest;
import com.yunus.user.entity.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin paneli üzerinden kullanıcı listeleme, detay görüntüleme, rol değiştirme,
 * aktiflik yönetimi ve randevu geçmişi sorgulama işlemlerini tanımlayan servis arayüzü.
 */
public interface AdminUserService {

    /**
     * Sistemdeki tüm kullanıcıları liste olarak döner.
     * Hassas alanlar (passwordHash) dahil edilmez.
     *
     * @return Tüm kullanıcıların detay yanıt listesi
     */
    List<AdminUserDetailResponse> getAllUsers();

    /**
     * Belirtilen kimliğe sahip kullanıcının detay bilgilerini getirir.
     * Kullanıcı bulunamazsa HTTP 404 fırlatır.
     *
     * @param id Sorgulanacak kullanıcının kimliği
     * @return Kullanıcı detay yanıt DTO'su
     */
    AdminUserDetailResponse getUserDetail(UUID id);

    /**
     * Belirtilen kullanıcının rolünü günceller.
     * Kullanıcı bulunamazsa HTTP 404 fırlatır.
     *
     * @param id   Rolü değiştirilecek kullanıcının kimliği
     * @param role Atanacak yeni rol
     */
    void updateUserRole(UUID id, UserRole role);

    /**
     * Belirtilen kullanıcıyı pasife alır (isActive = false — soft delete).
     * Kullanıcı bulunamazsa HTTP 404 fırlatır.
     *
     * @param id Pasife alınacak kullanıcının kimliği
     */
    void deactivateUser(UUID id);

    /**
     * Pasifteki kullanıcıyı yeniden aktif hale getirir (isActive = true).
     * Kullanıcı bulunamazsa HTTP 404 fırlatır.
     *
     * @param id Aktifleştirilecek kullanıcının kimliği
     */
    void activateUser(UUID id);

    /**
     * Belirtilen kullanıcıya ait randevuları sayfalanmış olarak döner.
     * Kullanıcı bulunamazsa HTTP 404 fırlatır.
     *
     * @param id       Randevuları listelenecek kullanıcının kimliği
     * @param pageable Sayfalama ve sıralama parametreleri
     * @return Sayfalanmış randevu özet listesi
     */
    Page<AdminUserAppointmentResponse> getUserAppointments(UUID id, Pageable pageable);
}
