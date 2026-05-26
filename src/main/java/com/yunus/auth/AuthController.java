package com.yunus.auth;

import com.yunus.auth.dto.AuthResponse;
import com.yunus.auth.dto.LoginRequest;
import com.yunus.auth.dto.RefreshTokenRequest;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.common.response.BaseResponse;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kimlik doğrulama, kayıt, çıkış ve profil bilgi API endpoint'lerini barındıran controller.
 * Korunan endpoint'ler: POST /register, POST /login, POST /refresh, POST /logout, GET /me
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    /**
     * Yeni bir kullanıcı kaydeder.
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(BaseResponse.success("Kullanıcı kaydı başarıyla oluşturuldu", response));
    }

    /**
     * Kullanıcı girişi sağlar.
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(BaseResponse.success("Giriş başarılı", response));
    }

    /**
     * Refresh token ile oturum yeniler (rotasyonlu token döner).
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(BaseResponse.success("Oturum başarıyla yenilendi", response));
    }

    /**
     * Kullanıcı oturumunu kapatır, refresh token'ı iptal eder.
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(@Valid @RequestBody com.yunus.auth.dto.LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(BaseResponse.success("Oturum kapatıldı"));
    }

    /**
     * Giriş yapmış kullanıcının profil detaylarını döner.
     * GET /api/v1/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserInfoResponse>> me() {
        User currentUser = currentUserService.getCurrentUser();
        UserInfoResponse response = authService.getUserInfo(currentUser);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
