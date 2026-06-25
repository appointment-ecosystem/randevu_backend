package com.yunus.appointment.service;

import com.yunus.appointment.dto.AppointmentResponse;
import com.yunus.appointment.entity.Appointment;
import com.yunus.appointment.entity.AppointmentStatus;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.ForbiddenException;
import com.yunus.notification.service.NotificationService;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private com.yunus.business.repository.ServiceRepository serviceRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private SlotLockService slotLockService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID staffAId = UUID.randomUUID();
    private final UUID staffAUserId = UUID.randomUUID();
    private final UUID staffBId = UUID.randomUUID();
    private final UUID staffBUserId = UUID.randomUUID();

    private Business newBusiness() {
        User owner = new User();
        owner.setId(ownerId);
        owner.setFullName("İşletme Sahibi");

        Business business = new Business();
        business.setId(businessId);
        business.setName("Ahmet Berber");
        business.setOwner(owner);
        return business;
    }

    private Staff newStaff(UUID staffId, UUID userId, Business business) {
        User staffUser = new User();
        staffUser.setId(userId);

        Staff staff = new Staff();
        staff.setId(staffId);
        staff.setFullName("Personel");
        staff.setBusiness(business);
        staff.setUser(staffUser);
        return staff;
    }

    private Appointment newAppointment(Business business, Staff staff) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Müşteri");

        com.yunus.business.entity.Service service = new com.yunus.business.entity.Service();
        service.setId(UUID.randomUUID());
        service.setName("Saç Kesimi");
        service.setDurationMin(30);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setUser(user);
        appointment.setBusiness(business);
        appointment.setService(service);
        appointment.setStaff(staff);
        appointment.setStartTime(OffsetDateTime.now());
        appointment.setEndTime(OffsetDateTime.now().plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointment;
    }

    @Test
    void ownerSeesAllBusinessAppointmentsIncludingUnassigned() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Appointment assigned = newAppointment(business, staffA);
        Appointment unassigned = newAppointment(business, null);

        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
        when(businessRepository.findByIdAndOwnerIdAndIsActiveTrue(businessId, ownerId))
                .thenReturn(Optional.of(business));
        when(appointmentRepository.findByBusinessIdAndTimeRange(any(), any(), any()))
                .thenReturn(List.of(assigned, unassigned));

        List<AppointmentResponse> result = appointmentService.getBusinessAppointments(
                businessId, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));

        assertEquals(2, result.size());
    }

    @Test
    void staffOnlySeesAppointmentsAssignedToThem() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Staff staffB = newStaff(staffBId, staffBUserId, business);
        Appointment assignedToA = newAppointment(business, staffA);
        Appointment assignedToB = newAppointment(business, staffB);
        Appointment unassigned = newAppointment(business, null);

        when(currentUserService.getCurrentUserId()).thenReturn(staffAUserId);
        when(businessRepository.findByIdAndOwnerIdAndIsActiveTrue(businessId, staffAUserId))
                .thenReturn(Optional.empty());
        when(staffRepository.findByUserIdAndIsActiveTrue(staffAUserId)).thenReturn(Optional.of(staffA));
        when(appointmentRepository.findByBusinessIdAndTimeRange(any(), any(), any()))
                .thenReturn(List.of(assignedToA, assignedToB, unassigned));

        List<AppointmentResponse> result = appointmentService.getBusinessAppointments(
                businessId, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));

        assertEquals(1, result.size());
        assertEquals(assignedToA.getId(), result.get(0).id());
    }

    @Test
    void staffCannotConfirmAppointmentNotAssignedToThem() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Staff staffB = newStaff(staffBId, staffBUserId, business);
        Appointment appointment = newAppointment(business, staffB);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(staffAUserId);
        when(staffRepository.findByUserIdAndIsActiveTrue(staffAUserId)).thenReturn(Optional.of(staffA));

        assertThrows(ForbiddenException.class,
                () -> appointmentService.confirmAppointment(appointment.getId()));
    }

    @Test
    void staffCanConfirmAppointmentAssignedToThem() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Appointment appointment = newAppointment(business, staffA);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(staffAUserId);
        when(staffRepository.findByUserIdAndIsActiveTrue(staffAUserId)).thenReturn(Optional.of(staffA));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.confirmAppointment(appointment.getId());

        assertEquals(AppointmentStatus.CONFIRMED, response.status());
    }

    @Test
    void staffFromAnotherBusinessCannotAccessTheseAppointments() {
        Business otherBusiness = new Business();
        otherBusiness.setId(UUID.randomUUID());
        otherBusiness.setName("Başka Berber");
        Staff staffOfOtherBusiness = newStaff(staffBId, staffBUserId, otherBusiness);

        when(currentUserService.getCurrentUserId()).thenReturn(staffBUserId);
        when(businessRepository.findByIdAndOwnerIdAndIsActiveTrue(businessId, staffBUserId))
                .thenReturn(Optional.empty());
        when(staffRepository.findByUserIdAndIsActiveTrue(staffBUserId))
                .thenReturn(Optional.of(staffOfOtherBusiness));

        assertThrows(ForbiddenException.class, () -> appointmentService.getBusinessAppointments(
                businessId, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1)));
    }

    @Test
    void nonOwnerNonStaffCannotAccessBusinessAppointments() {
        UUID strangerId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId()).thenReturn(strangerId);
        when(businessRepository.findByIdAndOwnerIdAndIsActiveTrue(businessId, strangerId))
                .thenReturn(Optional.empty());
        when(staffRepository.findByUserIdAndIsActiveTrue(strangerId)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> appointmentService.getBusinessAppointments(
                businessId, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1)));
    }

    @Test
    void customerCanViewOwnAppointment() {
        Business business = newBusiness();
        Appointment appointment = newAppointment(business, null);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(appointment.getUser().getId());

        AppointmentResponse response = appointmentService.getAppointment(appointment.getId());

        assertEquals(appointment.getId(), response.id());
    }

    @Test
    void assignedStaffCanViewAppointment() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Appointment appointment = newAppointment(business, staffA);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(staffAUserId);
        when(staffRepository.findByUserIdAndIsActiveTrue(staffAUserId)).thenReturn(Optional.of(staffA));

        AppointmentResponse response = appointmentService.getAppointment(appointment.getId());

        assertEquals(appointment.getId(), response.id());
    }

    @Test
    void unassignedStaffFromSameBusinessCannotViewAppointment() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Staff staffB = newStaff(staffBId, staffBUserId, business);
        Appointment appointment = newAppointment(business, staffB);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(staffAUserId);
        when(staffRepository.findByUserIdAndIsActiveTrue(staffAUserId)).thenReturn(Optional.of(staffA));

        assertThrows(ForbiddenException.class,
                () -> appointmentService.getAppointment(appointment.getId()));
    }

    @Test
    void ownerCanAlwaysViewAppointment() {
        Business business = newBusiness();
        Staff staffA = newStaff(staffAId, staffAUserId, business);
        Appointment appointment = newAppointment(business, staffA);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(currentUserService.getCurrentUserId()).thenReturn(ownerId);

        AppointmentResponse response = appointmentService.getAppointment(appointment.getId());

        assertEquals(appointment.getId(), response.id());
    }
}
