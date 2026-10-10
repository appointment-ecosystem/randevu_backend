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
 * Aynı slot için 10 paralel randevu isteği eşzamanlılık testi.
 * Senaryo (i): Redis kilidi devredeyken (tam 1 başarılı, 9'u 409)
 * Senaryo (ii): Redis kilidi devre dışıyken / by-passed (yalnızca DB index devrede -> tam 1 başarılı, 9'u 409)
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
        service.setName("Saç Kesimi");
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
    @DisplayName("Senaryo (i): Redis kilidi devredeyken 10 paralel istekten tam 1'i başarılı, 9'u 409 olmalıdır")
    void testConcurrencyWithRedisLock() throws InterruptedException {
        // Redis mock/fallback: ilk gelen thread kilidi alır, diğer 9 thread alamaz (SETNX davranışı simülasyonu)
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

        assertEquals(1, successCount.get(), "Tam olarak 1 istek başarılı olmalıdır");
        assertEquals(9, conflictCount.get(), "Tam 9 istek slot çakışması (409) almalıdır");
    }

    @Test
    @DisplayName("Senaryo (ii): Redis kilidi devre dışıyken DB Unique Constraint sayesinde yine tam 1 başarılı, 9'u 409 olmalıdır")
    void testConcurrencyWithDatabaseIndexOnly() throws InterruptedException {
        // Redis kilidini bypass et: her zaman true dönsün (kilit devre dışı)
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
                    if (e.getMessage() != null && (e.getMessage().contains("slot") || e.getMessage().contains("uk_appointments") || e.getMessage().contains("ConstraintViolation") || e.getMessage().contains("DataIntegrityViolation"))) {
                        conflictCount.incrementAndGet();
                    } else if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException || e.getCause() instanceof org.springframework.dao.DataIntegrityViolationException) {
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

        assertEquals(1, successCount.get(), "DB seviyesinde tam 1 istek başarılı olmalıdır");
        assertEquals(9, conflictCount.get(), "DB seviyesinde 9 istek çakışma (409) almalıdır");
    }
}
