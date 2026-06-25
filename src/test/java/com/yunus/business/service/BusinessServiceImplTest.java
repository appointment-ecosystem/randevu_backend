package com.yunus.business.service;

import com.yunus.business.config.BusinessProperties;
import com.yunus.business.dto.BusinessResponse;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessCategoryRepository;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.location.repository.CityRepository;
import com.yunus.location.repository.DistrictRepository;
import com.yunus.location.repository.NeighborhoodRepository;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import com.yunus.webhook.WebhookService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private NeighborhoodRepository neighborhoodRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private BusinessProperties businessProperties;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private BusinessServiceImpl businessService;

    @Test
    void shouldReturnBusinessFromStaffRecordWhenUserIsNotAnOwner() {
        UUID employeeUserId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();

        User employeeUser = new User();
        employeeUser.setId(employeeUserId);
        employeeUser.setFullName("Personel Ali");
        employeeUser.setRole(UserRole.BUSINESS_EMPLOYEE);
        employeeUser.setIsActive(true);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFullName("İşletme Sahibi");

        Business business = new Business();
        business.setId(businessId);
        business.setName("Ahmet Berber");
        business.setOwner(owner);
        business.setCategories(Collections.emptySet());

        Staff staff = new Staff();
        staff.setId(UUID.randomUUID());
        staff.setBusiness(business);

        when(userRepository.findById(employeeUserId)).thenReturn(Optional.of(employeeUser));
        when(businessRepository.findByOwnerId(employeeUserId)).thenReturn(List.of());
        when(staffRepository.findByUserIdAndIsActiveTrue(employeeUserId)).thenReturn(Optional.of(staff));

        List<BusinessResponse> result = businessService.getMyBusinesses(employeeUserId);

        assertEquals(1, result.size());
        assertEquals(businessId, result.get(0).id());
    }

    @Test
    void shouldReturnEmptyListWhenNeitherOwnedNorStaffBusinessFound() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setIsActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(businessRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(staffRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

        List<BusinessResponse> result = businessService.getMyBusinesses(userId);

        assertTrue(result.isEmpty());
    }
}
