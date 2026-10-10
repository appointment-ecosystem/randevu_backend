package com.yunus.auth;

import com.yunus.auth.dto.AuthResponse;
import com.yunus.auth.dto.LoginRequest;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.UnauthorizedException;
import com.yunus.common.response.BaseResponse;
import com.yunus.exception.GlobalExceptionHandler;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthController authController;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("200 OK: Geçerli kayıt isteği 200 döner")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setPhone("05551234567");
        request.setPassword("Password@123");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId(UUID.randomUUID())
                .role(UserRole.USER)
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        ResponseEntity<BaseResponse<AuthResponse>> response = authController.register(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("access-token", response.getBody().getData().getAccessToken());
    }

    @Test
    @DisplayName("401 Unauthorized ayrımı: Kimlik doğrulanmamışsa UnauthorizedException fırlatılır ve 401 olarak işlenir")
    void unauthorizedHandling() {
        when(authService.login(any(LoginRequest.class))).thenThrow(new UnauthorizedException("Geçersiz telefon numarası veya şifre"));

        LoginRequest request = new LoginRequest();
        request.setPhone("05551234567");
        request.setPassword("wrong-pass");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authController.login(request));
        ResponseEntity<BaseResponse<Void>> handled = exceptionHandler.handleUnauthorized(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, handled.getStatusCode());
    }

    @Test
    @DisplayName("403 Forbidden ayrımı: Yetkisiz erişimde ForbiddenException fırlatılır ve 403 olarak işlenir")
    void forbiddenHandling() {
        when(currentUserService.getCurrentUser()).thenThrow(new ForbiddenException("Bu işlem için yetkiniz yok"));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authController.me());
        ResponseEntity<BaseResponse<Void>> handled = exceptionHandler.handleForbidden(ex);
        assertEquals(HttpStatus.FORBIDDEN, handled.getStatusCode());
    }
}
