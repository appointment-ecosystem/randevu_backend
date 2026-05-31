package com.yunus.auth;

import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.auth.dto.AdminUserAppointmentResponse;
import com.yunus.auth.dto.AdminUserDetailResponse;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.exception.ErrorType;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin paneli kullanıcı yönetimi servis arayüzünün (AdminUserService) iş mantığı uygulaması.
 * Kullanıcı varlık kontrolü, entity-DTO dönüşümü ve randevu geçmişi sorgulama işlemlerini yönetir.
 */
@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Bağımlılıkları constructor üzerinden enjekte eden yapıcı metot.
     *
     * @param userRepository        Kullanıcı veri erişim repository nesnesi
     * @param appointmentRepository Randevu veri erişim repository nesnesi
     */
    public AdminUserServiceImpl(UserRepository userRepository,
                                AppointmentRepository appointmentRepository) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Sistemdeki tüm kullanıcıları AdminUserDetailResponse listesi olarak döner.
     * Salt okunur işlem olduğundan readOnly transaction kullanılır.
     *
     * @return Tüm kullanıcıların detay yanıt listesi
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDetailResponse> getAllUsers() {
        log.info("Admin tüm kullanıcıları listeleme isteği");
        return userRepository.findAll()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    /**
     * Belirtilen kimliğe sahip kullanıcının detay bilgilerini getirir.
     * Kullanıcı bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id Sorgulanacak kullanıcının kimliği
     * @return Kullanıcı detay yanıt DTO'su
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(UUID id) {
        log.info("Admin kullanıcı detay isteği — id: {}", id);
        User user = findUserById(id);
        return toDetailResponse(user);
    }

    /**
     * Belirtilen kullanıcının rolünü günceller.
     * Kullanıcı bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id   Rolü değiştirilecek kullanıcının kimliği
     * @param role Atanacak yeni rol
     */
    @Override
    public void updateUserRole(UUID id, UserRole role) {
        log.info("Admin kullanıcı rol güncelleme — id: {}, yeni rol: {}", id, role);
        User user = findUserById(id);
        user.setRole(role);
        userRepository.save(user);
        log.info("Kullanıcı rolü güncellendi — id: {}", id);
    }

    /**
     * Belirtilen kullanıcıyı pasife alır (isActive = false — soft delete).
     * Kullanıcı bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id Pasife alınacak kullanıcının kimliği
     */
    @Override
    public void deactivateUser(UUID id) {
        log.info("Admin kullanıcı pasife alma isteği — id: {}", id);
        User user = findUserById(id);
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Kullanıcı pasife alındı — id: {}", id);
    }

    /**
     * Pasifteki kullanıcıyı yeniden aktif hale getirir (isActive = true).
     * Kullanıcı bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id Aktifleştirilecek kullanıcının kimliği
     */
    @Override
    public void activateUser(UUID id) {
        log.info("Admin kullanıcı aktifleştirme isteği — id: {}", id);
        User user = findUserById(id);
        user.setIsActive(true);
        userRepository.save(user);
        log.info("Kullanıcı aktifleştirildi — id: {}", id);
    }

    /**
     * Belirtilen kullanıcıya ait randevuları başlangıç zamanına göre azalan sırada
     * sayfalanmış olarak döner.
     * Kullanıcı bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id       Randevuları listelenecek kullanıcının kimliği
     * @param pageable Sayfalama ve sıralama parametreleri
     * @return Sayfalanmış randevu özet listesi
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserAppointmentResponse> getUserAppointments(UUID id, Pageable pageable) {
        log.info("Admin kullanıcı randevuları isteği — id: {}", id);
        // Kullanıcının varlığını doğrula; yoksa 404 fırlat
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorType.USER_NOT_FOUND, "Kullanıcı bulunamadı: " + id);
        }
        return appointmentRepository.findByUserIdOrderByStartTimeDesc(id, pageable)
                .map(this::toAppointmentResponse);
    }

    // ─── Yardımcı metodlar ────────────────────────────────────────────────────

    /**
     * Kimlik bilgisine göre kullanıcıyı veri tabanından sorgular.
     * Bulunamazsa ResourceNotFoundException (HTTP 404) fırlatır.
     *
     * @param id Sorgulanacak kullanıcının kimliği
     * @return Bulunan User entity nesnesi
     */
    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorType.USER_NOT_FOUND, "Kullanıcı bulunamadı: " + id));
    }

    /**
     * User entity nesnesini AdminUserDetailResponse DTO'suna dönüştürür.
     * Hassas alanlar (passwordHash) DTO'ya dahil edilmez.
     *
     * @param user Dönüştürülecek User entity nesnesi
     * @return Kullanıcı detay yanıt DTO'su
     */
    private AdminUserDetailResponse toDetailResponse(User user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getPhoneVerified(),
                user.getRole(),
                user.getIsActive(),
                user.getProfilePhotoUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Appointment entity nesnesini AdminUserAppointmentResponse DTO'suna dönüştürür.
     * İlişkili entity alanları (business.name, service.name, staff.fullName) düzleştirilir.
     * Personel atanmamış randevularda staffName null olarak set edilir.
     *
     * @param appointment Dönüştürülecek Appointment entity nesnesi
     * @return Randevu özet yanıt DTO'su
     */
    private AdminUserAppointmentResponse toAppointmentResponse(Appointment appointment) {
        String staffName = (appointment.getStaff() != null)
                ? appointment.getStaff().getFullName()
                : null;

        return new AdminUserAppointmentResponse(
                appointment.getId(),
                appointment.getBusiness().getName(),
                appointment.getService().getName(),
                staffName,
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getPriceSnapshot()
        );
    }
}
