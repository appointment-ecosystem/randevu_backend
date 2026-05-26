package com.yunus.auth.service;

import com.yunus.auth.dto.RegisterRequest;
import com.yunus.common.exception.ConflictException;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowConflictExceptionWhenPhoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Yunus Emre");
        request.setPhone("5554443322");
        request.setEmail("yunus@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.USER);

        // Given
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () -> authService.register(request));
    }
}
