package com.yunus.business.validator;

import com.yunus.business.entity.Business;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessOwnershipValidatorTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BusinessOwnershipValidator validator;

    private UUID businessId;
    private UUID ownerId;
    private UUID strangerId;
    private Business business;
    private User owner;
    private User stranger;
    private User admin;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);
        owner.setRole(UserRole.BUSINESS_OWNER);

        stranger = new User();
        stranger.setId(strangerId);
        stranger.setRole(UserRole.BUSINESS_OWNER);

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        business = new Business();
        business.setId(businessId);
        business.setOwner(owner);
    }

    @Test
    @DisplayName("İşletme sahibi doğrulandığında başarıyla Business döner")
    void ownerCanValidateSuccessfully() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        Business result = validator.validateOwnership(businessId);
        assertNotNull(result);
        assertEquals(businessId, result.getId());
    }

    @Test
    @DisplayName("Admin kullanıcısı herhangi bir işletmeyi doğrulayabilir")
    void adminCanValidateAnyBusiness() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        Business result = validator.validateOwnership(businessId);
        assertNotNull(result);
        assertEquals(businessId, result.getId());
    }

    @Test
    @DisplayName("Başka bir işletmenin sahibi (stranger) işlem yapmaya çalıştığında 403 Forbidden alır")
    void strangerReceivesForbidden() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(stranger);

        assertThrows(ForbiddenException.class, () -> validator.validateOwnership(businessId));
    }

    @Test
    @DisplayName("İşletme mevcut değilse 404 ResourceNotFoundException fırlatır")
    void notFoundThrowsException() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> validator.validateOwnership(businessId));
    }
}
