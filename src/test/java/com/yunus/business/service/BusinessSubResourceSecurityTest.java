package com.yunus.business.service;

import com.yunus.business.dto.BulkWorkingHourRequest;
import com.yunus.business.dto.CreateServiceRequest;
import com.yunus.business.dto.HolidayRequest;
import com.yunus.business.dto.UpdateServiceRequest;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.BusinessPhoto;
import com.yunus.business.entity.Holiday;
import com.yunus.business.entity.Service;
import com.yunus.business.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessSubResourceSecurityTest {

    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private WorkingHourRepository workingHourRepository;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private BusinessPhotoRepository businessPhotoRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private CurrentUserService currentUserService;

    private ServiceManagementServiceImpl serviceManagementService;
    private WorkingHourServiceImpl workingHourService;
    private HolidayServiceImpl holidayService;
    private BusinessPhotoServiceImpl businessPhotoService;

    private UUID businessId;
    private UUID strangerBusinessId;
    private Business business;
    private User owner;
    private User stranger;

    @BeforeEach
    void setUp() {
        serviceManagementService = new ServiceManagementServiceImpl(businessRepository, serviceRepository, storageService, currentUserService);
        workingHourService = new WorkingHourServiceImpl(businessRepository, staffRepository, workingHourRepository, currentUserService);
        holidayService = new HolidayServiceImpl(businessRepository, staffRepository, holidayRepository, currentUserService);
        businessPhotoService = new BusinessPhotoServiceImpl(businessRepository, businessPhotoRepository, storageService, currentUserService);

        businessId = UUID.randomUUID();
        strangerBusinessId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());

        stranger = new User();
        stranger.setId(UUID.randomUUID());

        business = new Business();
        business.setId(businessId);
        business.setOwner(owner);
    }

    // --- ServiceManagementService ---
    @Test
    @DisplayName("ServiceManagement: Stranger owner gets 403 Forbidden on createService")
    void serviceManagement_strangerGetsForbidden() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(stranger);

        CreateServiceRequest req = new CreateServiceRequest("Saç Kesimi", "Açıklama", 30, BigDecimal.valueOf(100), "TRY", 1);
        assertThrows(ForbiddenException.class, () -> serviceManagementService.createService(businessId, req));
    }

    @Test
    @DisplayName("ServiceManagement: Updating service belonging to another business throws 404")
    void serviceManagement_subResourceBelongingToAnotherBusiness_throwsNotFound() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        UUID serviceId = UUID.randomUUID();
        Business otherBusiness = new Business();
        otherBusiness.setId(strangerBusinessId);

        Service strangerService = new Service();
        strangerService.setId(serviceId);
        strangerService.setBusiness(otherBusiness);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(strangerService));

        UpdateServiceRequest req = new UpdateServiceRequest("Saç Kesimi", "Açıklama", 30, BigDecimal.valueOf(100), "TRY", 1);
        assertThrows(ResourceNotFoundException.class, () -> serviceManagementService.updateService(businessId, serviceId, req));
    }

    // --- WorkingHourService ---
    @Test
    @DisplayName("WorkingHour: Stranger owner gets 403 Forbidden on setBusinessHours")
    void workingHour_strangerGetsForbidden() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(stranger);

        BulkWorkingHourRequest req = new BulkWorkingHourRequest(List.of());
        assertThrows(ForbiddenException.class, () -> workingHourService.setBusinessHours(businessId, req));
    }

    // --- HolidayService ---
    @Test
    @DisplayName("Holiday: Stranger owner gets 403 Forbidden on addHoliday")
    void holiday_strangerGetsForbidden() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(stranger);

        HolidayRequest req = new HolidayRequest(LocalDate.now().plusDays(5), "Yılbaşı", null);
        assertThrows(ForbiddenException.class, () -> holidayService.addHoliday(businessId, req));
    }

    @Test
    @DisplayName("Holiday: Deleting holiday belonging to another business throws 404")
    void holiday_subResourceBelongingToAnotherBusiness_throwsNotFound() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        UUID holidayId = UUID.randomUUID();
        Business otherBusiness = new Business();
        otherBusiness.setId(strangerBusinessId);

        Holiday strangerHoliday = new Holiday();
        strangerHoliday.setId(holidayId);
        strangerHoliday.setBusiness(otherBusiness);

        when(holidayRepository.findById(holidayId)).thenReturn(Optional.of(strangerHoliday));

        assertThrows(ForbiddenException.class, () -> holidayService.deleteHoliday(businessId, holidayId));
    }

    // --- BusinessPhotoService ---
    @Test
    @DisplayName("BusinessPhoto: Deleting photo belonging to another business throws 404")
    void businessPhoto_subResourceBelongingToAnotherBusiness_throwsNotFound() {
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        UUID photoId = UUID.randomUUID();
        Business otherBusiness = new Business();
        otherBusiness.setId(strangerBusinessId);

        BusinessPhoto strangerPhoto = new BusinessPhoto();
        strangerPhoto.setId(photoId);
        strangerPhoto.setBusiness(otherBusiness);

        when(businessPhotoRepository.findById(photoId)).thenReturn(Optional.of(strangerPhoto));

        assertThrows(ResourceNotFoundException.class, () -> businessPhotoService.deletePhoto(businessId, photoId));
    }
}