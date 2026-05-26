package com.yunus.auth;

import com.yunus.auth.dto.UserRoleUpdateRequest;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.common.response.BaseResponse;
import com.yunus.user.entity.User;
import com.yunus.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin kullanıcı yönetimi endpoint'lerini barındıran controller.
 * Tüm endpoint'ler ADMIN rolü gerektirir.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tüm kullanıcıları listeler.
     * GET /api/v1/admin/users
     */
    @GetMapping
    public ResponseEntity<BaseResponse<java.util.List<User>>> listUsers() {
        java.util.List<User> users = userRepository.findAll();
        return ResponseEntity.ok(BaseResponse.success(users));
    }

    /**
     * Belirtilen kullanıcının rolünü günceller.
     * PATCH /api/v1/admin/users/{userId}/role
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<BaseResponse<Void>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));

        log.info("Admin updating role for user {} from {} to {}", userId, user.getRole(), request.getRole());
        user.setRole(request.getRole());
        userRepository.save(user);

        return ResponseEntity.ok(BaseResponse.success("Kullanıcı rolü başarıyla güncellendi"));
    }

    /**
     * Belirtilen kullanıcıyı devre dışı bırakır (soft delete).
     * PATCH /api/v1/admin/users/{userId}/deactivate
     */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<BaseResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));

        log.info("Admin deactivating user: {}", userId);
        user.setIsActive(false);
        userRepository.save(user);

        return ResponseEntity.ok(BaseResponse.success("Kullanıcı devre dışı bırakıldı"));
    }

    /**
     * Belirtilen kullanıcıyı yeniden aktif eder.
     * PATCH /api/v1/admin/users/{userId}/activate
     */
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<BaseResponse<Void>> activateUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "id", userId));

        log.info("Admin activating user: {}", userId);
        user.setIsActive(true);
        userRepository.save(user);

        return ResponseEntity.ok(BaseResponse.success("Kullanıcı aktif edildi"));
    }
}
