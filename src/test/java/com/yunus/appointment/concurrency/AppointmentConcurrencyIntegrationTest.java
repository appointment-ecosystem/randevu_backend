package com.yunus.appointment.concurrency;

import com.yunus.appointment.dto.CreateAppointmentRequest;
import com.yunus.appointment.repository.AppointmentRepository;
import com.yunus.appointment.service.AppointmentService;
import com.yunus.appointment.service.SlotLockService;
import com.yunus.business.entity.Business;
import com.yunus.business.entity.Service;
import com.yunus.business.entity.Staff;
import com.yunus.business.repository.BusinessRepository;
import com.yunus.business.repository.ServiceRepository;
import com.yunus.business.repository.StaffRepository;
import com.yunus.common.exception.ConflictException;
import com.yunus.common.exception.SlotAlreadyTakenException;
import com.yunus.security.CurrentUserService;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import com.yunus.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ayni slot icin 10 paralel randevu istegi esszamanlilik testi.
 * Senaryo (i): Redis kilidi devredeyken (tam 1 basarili, 9u 409)
 * Senaryo (ii): Redis kilidi devre disiyken (yalnizca DB index devrede -> tam 1 basarili, 9u 409)
 */
@SpringBootTest
@ActiveProfiles("test")
public class AppointmentConcurrencyIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private SlotLockService slotLockService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private Business business;
    private Service service;
    private Staff staff;
    private final List<User> testUsers = new ArrayList<>();
    private final OffsetDateTime slotTime = OffsetDateTime.of(2026, 12, 1, 14, 0, 0, 0, ZoneOffset.ofHours(3));

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        testUsers.clear();

        long rand = System.currentTimeMillis() % 1000000;
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setFullName("User " + i);
            user.setPhone(String.format("055%07d", rand + i));
            user.setPasswordHash("hash" + i);
            user.setRole(UserRole.USER);
            user.setIsActive(true);
            testUsers.add(userRepository.save(user));
        }

        User owner = testUsers.get(0);

        business = new Business();
        business.setName("Test Barber " + rand);
        business.setSlug("test-barber-" + UUID.randomUUID());
        business.setOwner(owner);
        business.setIsActive(true);
        business = businessRepository.save(business);

        service = new Service();
        service.setBusiness(business);
        service.setName("Sac Kesimi");
        service.setDurationMin(30);
        service.setPrice(BigDecimal.valueOf(100));
        service.setIsActive(true);
        service = serviceRepository.save(service);

        staff = new Staff();
        staff.setBusiness(business);
        staff.setFullName("Ahmet Usta");
        staff.setIsActive(true);
        staff = staffRepository.save(staff);
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
    }

    @Test
    @DisplayName("Senaryo (i): Redis kilidi devredeyken 10 paralel istekten tam 1i basarili, 9u 409 olmalidir")
    void testConcurrencyWithRedisLock() throws InterruptedException {
        ConcurrentHashMap<String, String> inMemoryLockStore = new ConcurrentHashMap<>();
        Mockito.doAnswer(invocation -> {
            UUID bId = invocation.getArgument(0);
            UUID sId = invocation.getArgument(1);
            OffsetDateTime sTime = invocation.getArgument(2);
            UUID uId = invocation.getArgument(3);
            String lockKey = "slot:lock:" + bId + ":" + sId + ":" + sTime;
            return inMemoryLockStore.putIfAbsent(lockKey, uId.toString()) == null;
        }).when(slotLockService).tryLock(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    User currentUser = testUsers.get(index);
                    Mockito.when(currentUserService.getCurrentUser()).thenReturn(currentUser);

                    CreateAppointmentRequest request = new CreateAppointmentRequest(
                            business.getId(),
                            service.getId(),
                            staff.getId(),
                            slotTime,
                            "Not " + index
                    );

                    appointmentService.createAppointment(request);
                    successCount.incrementAndGet();
                } catch (SlotAlreadyTakenException | ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("slot")) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Tam olarak 1 istek basarili olmalidir");
        assertEquals(9, conflictCount.get(), "Tam 9 istek slot catismasi (409) almalidir");
    }

    @Test
    @DisplayName("Senaryo (ii): Redis kilidi devre disiyken DB Unique Constraint sayesinde yine tam 1 basarili, 9u 409 olmalidir")
    void testConcurrencyWithDatabaseIndexOnly() throws InterruptedException {
        Mockito.doReturn(true).when(slotLockService).tryLock(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    User currentUser = testUsers.get(index);
                    Mockito.when(currentUserService.getCurrentUser()).thenReturn(currentUser);

                    CreateAppointmentRequest request = new CreateAppointmentRequest(
                            business.getId(),
                            service.getId(),
                            staff.getId(),
                            slotTime,
                            "DB Only Not " + index
                    );

                    appointmentService.createAppointment(request);
                    successCount.incrementAndGet();
                } catch (SlotAlreadyTakenException | ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("slot")
                            || e.getMessage().contains("uk_appointments")
                            || e.getMessage().contains("ConstraintViolation")
                            || e.getMessage().contains("DataIntegrityViolation"))) {
                        conflictCount.incrementAndGet();
                    } else if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException
                            || e.getCause() instanceof org.springframework.dao.DataIntegrityViolationException) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "DB seviyesinde tam 1 istek basarili olmalidir");
        assertEquals(9, conflictCount.get(), "DB seviyesinde 9 istek catisma (409) almalidir");
    }

    @Test
    @DisplayName("Cakisan aralik testi: 10:00-10:30 varken 10:15-10:45 SlotAlreadyTakenException, 10:30-11:00 basarili, iptal sonrasi slot serbest")
    void testOverlappingIntervalsAndCancellation() {
        // Bu test DB katmanindaki cakisma tespitini (existsOverlappingByStaff JPQL sorgusu) test eder.
        // Redis kilidi tamamen bypass edilmistir (tryLock=true, releaseLock=no-op).
        // OffsetDateTime zone-offset farki (test-time vs DB-return) sonucu etkilemez.
        Mockito.reset(slotLockService);
        Mockito.doReturn(true).when(slotLockService)
                .tryLock(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(slotLockService)
                .releaseLock(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        OffsetDateTime t10_00 = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t10_15 = OffsetDateTime.of(2026, 12, 1, 10, 15, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t10_30 = OffsetDateTime.of(2026, 12, 1, 10, 30, 0, 0, ZoneOffset.ofHours(3));

        // 1. 10:00-10:30 randevusu olustur (hizmet suresi 30 dk)
        Mockito.when(currentUserService.getCurrentUser()).thenReturn(testUsers.get(0));
        Mockito.when(currentUserService.getCurrentUserId()).thenReturn(testUsers.get(0).getId());
        CreateAppointmentRequest req1 = new CreateAppointmentRequest(
                business.getId(), service.getId(), staff.getId(), t10_00, "10:00-10:30 randevusu");
        var app1 = appointmentService.createAppointment(req1);
        org.junit.jupiter.api.Assertions.assertNotNull(app1);

        // 2. 10:15-10:45 cakisan randevu -> DB overlap sorgusu SlotAlreadyTakenException firlatiyor
        Mockito.when(currentUserService.getCurrentUser()).thenReturn(testUsers.get(1));
        Mockito.when(currentUserService.getCurrentUserId()).thenReturn(testUsers.get(1).getId());
        CreateAppointmentRequest reqConflict = new CreateAppointmentRequest(
                business.getId(), service.getId(), staff.getId(), t10_15, "10:15-10:45 cakisan");
        org.junit.jupiter.api.Assertions.assertThrows(
                SlotAlreadyTakenException.class,
                () -> appointmentService.createAppointment(reqConflict),
                "10:00-10:30 ile cakisan 10:15-10:45 randevusu SlotAlreadyTakenException firlatmalidir"
        );

        // 3. 10:30-11:00 bitisik aralik (startTime == onceki endTime) -> catismiyor, basarili olmali
        Mockito.when(currentUserService.getCurrentUser()).thenReturn(testUsers.get(2));
        Mockito.when(currentUserService.getCurrentUserId()).thenReturn(testUsers.get(2).getId());
        CreateAppointmentRequest reqAdjacent = new CreateAppointmentRequest(
                business.getId(), service.getId(), staff.getId(), t10_30, "10:30-11:00 bitisik");
        var appAdjacent = appointmentService.createAppointment(reqAdjacent);
        org.junit.jupiter.api.Assertions.assertNotNull(appAdjacent);

        // 4. app1 iptal et (CANCELLED_BY_USER); DB overlap sorgusu cancelled statusu ACTIVE saymiyor
        Mockito.when(currentUserService.getCurrentUser()).thenReturn(testUsers.get(0));
        Mockito.when(currentUserService.getCurrentUserId()).thenReturn(testUsers.get(0).getId());
        appointmentService.cancelAppointmentByUser(app1.id(), "Kullanici plani degisti");

        // 5. Iptal sonrasi slotun yeniden alinabilirligi:
        //    CANCELLED_BY_USER durumundaki kayit DB'de durmasina ragmen, partial unique index ve
        //    existsOverlappingByStaff sorgusu yalnizca aktif randevulari (PENDING, CONFIRMED) hesaba katar.
        //    Dolayisiyla ayni slot baska bir kullanici tarafindan basariyla rezerve edilebilir.
        Mockito.when(currentUserService.getCurrentUser()).thenReturn(testUsers.get(3));
        Mockito.when(currentUserService.getCurrentUserId()).thenReturn(testUsers.get(3).getId());
        CreateAppointmentRequest reqRebook = new CreateAppointmentRequest(
                business.getId(), service.getId(), staff.getId(), t10_00, "Yeniden rezerve edilen slot");
        var appRebooked = appointmentService.createAppointment(reqRebook);
        org.junit.jupiter.api.Assertions.assertNotNull(appRebooked, "Iptal edilen slot tekrar rezerve edilebilmelidir");
    }

    @Test
    @DisplayName("Senaryo (v): DB-only V3 Exclusion Constraint - 10:00-11:00 varken 10:30-11:30 DB tarafından reddedilir (exclude_overlapping_appointments), 11:00-12:00 başarılı olur")
    void testDirectRepositorySaveThrowsDataIntegrityViolationOnConflict() {
        OffsetDateTime t10_00 = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t11_00 = OffsetDateTime.of(2026, 12, 1, 11, 0, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t10_30 = OffsetDateTime.of(2026, 12, 1, 10, 30, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t11_30 = OffsetDateTime.of(2026, 12, 1, 11, 30, 0, 0, ZoneOffset.ofHours(3));
        OffsetDateTime t12_00 = OffsetDateTime.of(2026, 12, 1, 12, 0, 0, 0, ZoneOffset.ofHours(3));

        // 1. İlk kayıt: 10:00 - 11:00 (CONFIRMED)
        com.yunus.appointment.entity.Appointment first = new com.yunus.appointment.entity.Appointment();
        first.setUser(testUsers.get(0));
        first.setBusiness(business);
        first.setService(service);
        first.setStaff(staff);
        first.setStartTime(t10_00);
        first.setEndTime(t11_00);
        first.setStatus(com.yunus.appointment.entity.AppointmentStatus.CONFIRMED);
        first.setPriceSnapshot(service.getPrice());
        first.setCurrency("TRY");
        appointmentRepository.saveAndFlush(first);

        // 2. İkinci kayıt: 10:30 - 11:30 (kesişen aralık!) -> V3 exclusion constraint tarafından reddedilmeli
        com.yunus.appointment.entity.Appointment overlapping = new com.yunus.appointment.entity.Appointment();
        overlapping.setUser(testUsers.get(1));
        overlapping.setBusiness(business);
        overlapping.setService(service);
        overlapping.setStaff(staff);
        overlapping.setStartTime(t10_30);
        overlapping.setEndTime(t11_30);
        overlapping.setStatus(com.yunus.appointment.entity.AppointmentStatus.PENDING);
        overlapping.setPriceSnapshot(service.getPrice());
        overlapping.setCurrency("TRY");

        org.springframework.dao.DataIntegrityViolationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> appointmentRepository.saveAndFlush(overlapping),
                "10:00-11:00 ile kesişen 10:30-11:30 kaydı exclude_overlapping_appointments kısıtı tarafından reddedilmelidir"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().contains("exclude_overlapping_appointments"),
                "Hata mesajı exclude_overlapping_appointments kısıtını içermelidir. Alınan: " + ex.getMessage()
        );

        // 3. Üçüncü kayıt: 11:00 - 12:00 (bitişik aralık) -> çakışma yok, başarılı olmalı
        com.yunus.appointment.entity.Appointment adjacent = new com.yunus.appointment.entity.Appointment();
        adjacent.setUser(testUsers.get(2));
        adjacent.setBusiness(business);
        adjacent.setService(service);
        adjacent.setStaff(staff);
        adjacent.setStartTime(t11_00);
        adjacent.setEndTime(t12_00);
        adjacent.setStatus(com.yunus.appointment.entity.AppointmentStatus.PENDING);
        adjacent.setPriceSnapshot(service.getPrice());
        adjacent.setCurrency("TRY");
        var savedAdjacent = appointmentRepository.saveAndFlush(adjacent);
        org.junit.jupiter.api.Assertions.assertNotNull(savedAdjacent.getId(), "Bitişik 11:00-12:00 randevusu başarıyla kaydedilmelidir");
    }
}