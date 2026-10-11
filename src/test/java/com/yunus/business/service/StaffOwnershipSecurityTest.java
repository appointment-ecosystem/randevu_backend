package com.yunus.business.service;

import com.yunus.business.dto.CreateStaffRequest;
import com.yunus.business.dto.UpdateStaffRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.common.exception.ResourceNotFoundException;
import com.yunus.security.CurrentUserService;
import com.yunus.storage.service.StorageService;
import com.yunus.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffOwnershipSecurityTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private CurrentUserService currentUserService;

    private StaffServiceImpl staffService;

    private UUID businessId;
    private UUID strangerBusinessId;
    private UUID staffId;
    private Business business;
    private Staff strangerStaff;
    private User owner;
    private User strangerUser;

    @BeforeEach
    void setUp() {
        staffService = new StaffServiceImpl(businessRepository, staffRepository, serviceRepository, storageService, currentUserService);

        businessId = UUID.randomUUID();
        strangerBusinessId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());

        strangerUser = new User();
        strangerUser.setId(UUID.randomUUID());

        business = new Business();
        business.setId(businessId);
        business.setOwner(owner);

        Business otherBusiness = new Business();
        otherBusiness.setId(strangerBusinessId);
        otherBusiness.setOwner(strangerUser);

        strangerStaff = new Staff();
        strangerStaff.setId(staffId);
        strangerStaff.setBusiness(otherBusiness); // Belongs to stranger business
    }

    @Test
    @DisplayName("Stranger business owner attempting write should receive 403 Forbidden")
    void createStaff_whenNotOwner_shouldThrowForbidden() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(strangerUser);

        CreateStaffRequest request = new CreateStaffRequest("Ahmet", "Berber", "Bio", 1);

        assertThrows(ForbiddenException.class, () -> staffService.createStaff(businessId, request));
        verifyNoInteractions(staffRepository);
    }

    @Test
    @DisplayName("Attempting to access sub-resource of another business should throw 404 ResourceNotFound")
    void updateStaff_whenSubResourceBelongsToAnotherBusiness_shouldThrowResourceNotFound() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        // The staff exists, but belongs to otherBusiness (not businessId)
        when(staffRepository.findById(staffId)).thenReturn(Optional.of(strangerStaff));

        UpdateStaffRequest request = new UpdateStaffRequest("Ahmet Güncel", "Usta", "Yeni Bio", 2);

        assertThrows(ResourceNotFoundException.class, () -> staffService.updateStaff(businessId, staffId, request));
    }
}