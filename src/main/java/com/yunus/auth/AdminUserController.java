package com.yunus.auth;

import com.yunus.auth.dto.AdminUserAppointmentResponse;
import com.yunus.auth.dto.AdminUserDetailResponse;
import com.yunus.auth.dto.UserRoleUpdateRequest;
import com.yunus.common.response.BaseResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin rolüne sahip kullanıcıların sistem genelindeki kullanıcı hesaplarını
 * listeleme, detay görüntüleme, rol değiştirme, aktiflik yönetimi ve
 * randevu geçmişi sorgulama işlemlerini yapabilmesini sağlayan denetleyici sınıf.
 * Tüm endpoint'ler ROLE_ADMIN yetkisi gerektirir.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Sınıf için bağımlılık enjeksiyonu yapıcı metodu.
     *
     * @param adminUserService Admin kullanıcı yönetim servisi
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Sistemdeki tüm kullanıcıları listeler.
     * GET /api/v1/admin/users
     *
     * @return Kullanıcı detay listesi yanıtı
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<AdminUserDetailResponse>>> listUsers() {
        List<AdminUserDetailResponse> response = adminUserService.getAllUsers();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen kimliğe sahip kullanıcının detay bilgilerini getirir.
     * GET /api/v1/admin/users/{id}
     *
     * @param id Sorgulanacak kullanıcının kimliği
     * @return Kullanıcı detay yanıtı
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<AdminUserDetailResponse>> getUserDetail(@PathVariable UUID id) {
        AdminUserDetailResponse response = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen kullanıcıya ait randevuları sayfalanmış olarak listeler.
     * GET /api/v1/admin/users/{id}/appointments
     *
     * @param id   Randevuları listelenecek kullanıcının kimliği
     * @param page Sayfa numarası (varsayılan 0)
     * @param size Sayfadaki kayıt sayısı (varsayılan 20)
     * @return Sayfalanmış randevu özet listesi yanıtı
     */
    @GetMapping("/{id}/appointments")
    public ResponseEntity<BaseResponse<Page<AdminUserAppointmentResponse>>> getUserAppointments(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminUserAppointmentResponse> response = adminUserService.getUserAppointments(id, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Belirtilen kullanıcının rolünü günceller.
     * PATCH /api/v1/admin/users/{userId}/role
     *
     * @param userId  Rolü değiştirilecek kullanıcının kimliği
     * @param request Yeni rol bilgisini içeren istek DTO'su
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<BaseResponse<Void>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {
        adminUserService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(BaseResponse.success("Kullanıcı rolü başarıyla güncellendi"));
    }

    /**
     * Belirtilen kullanıcıyı devre dışı bırakır (soft delete).
     * PATCH /api/v1/admin/users/{userId}/deactivate
     *
     * @param userId Pasife alınacak kullanıcının kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<BaseResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        adminUserService.deactivateUser(userId);
        return ResponseEntity.ok(BaseResponse.success("Kullanıcı devre dışı bırakıldı"));
    }

    /**
     * Belirtilen kullanıcıyı yeniden aktif eder.
     * PATCH /api/v1/admin/users/{userId}/activate
     *
     * @param userId Aktifleştirilecek kullanıcının kimliği
     * @return Başarılı işlem yanıtı
     */
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<BaseResponse<Void>> activateUser(@PathVariable UUID userId) {
        adminUserService.activateUser(userId);
        return ResponseEntity.ok(BaseResponse.success("Kullanıcı aktif edildi"));
    }
}
