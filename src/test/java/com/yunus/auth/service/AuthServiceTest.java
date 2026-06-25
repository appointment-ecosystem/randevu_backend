package com.yunus.auth.service;

import com.yunus.auth.AuthService;
import com.yunus.auth.dto.RegisterRequest;
import com.yunus.auth.dto.UserInfoResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.ConflictException;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowConflictExceptionWhenPhoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Yunus Emre");
        request.setPhone("5554443322");
        request.setEmail("yunus@example.com");
        request.setPassword("password123");
        // role alanı kaldırıldı — public register her zaman USER rolüyle kayıt yapar

        // Given
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // When & Then
        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void shouldReturnStaffAndBusinessIdWhenEmployeeHasActiveStaffRecord() {
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setFullName("Personel Ali");
        user.setRole(UserRole.BUSINESS_EMPLOYEE);

        Business business = new Business();
        business.setId(businessId);

        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setBusiness(business);

        when(staffRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.of(staff));

        UserInfoResponse response = authService.getUserInfo(user);

        assertEquals(staffId, response.getStaffId());
        assertEquals(businessId, response.getBusinessId());
    }

    @Test
    void shouldReturnNullStaffAndBusinessIdWhenEmployeeHasNoActiveStaffRecord() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setFullName("Personel Ayşe");
        user.setRole(UserRole.BUSINESS_EMPLOYEE);

        when(staffRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        UserInfoResponse response = authService.getUserInfo(user);

        assertNull(response.getStaffId());
        assertNull(response.getBusinessId());
    }
}
