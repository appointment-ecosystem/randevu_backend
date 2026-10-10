package com.yunus.appointment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotLockServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SlotLockService slotLockService;

    private final UUID businessId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final OffsetDateTime startTime = OffsetDateTime.of(2026, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3));
    private final String lockToken = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        slotLockService = new SlotLockService(redisTemplate);
    }

    @Test
    @DisplayName("tryAcquire: Kilit başarılı şekilde alındığında true döner")
    void tryAcquireSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq(lockToken), any(Duration.class))).thenReturn(true);

        boolean acquired = slotLockService.tryAcquire(businessId, staffId, startTime, lockToken);
        assertTrue(acquired);
    }

    @Test
    @DisplayName("tryAcquire: Slot zaten kilitliyse false döner")
    void tryAcquireAlreadyLocked() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq(lockToken), any(Duration.class))).thenReturn(false);

        boolean acquired = slotLockService.tryAcquire(businessId, staffId, startTime, lockToken);
        assertFalse(acquired);
    }

    @Test
    @DisplayName("release: Lua script ile atomik compare-and-delete başarılı olduğunda (1) true döner")
    void releaseSuccess() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(), eq(lockToken))).thenReturn(1L);

        boolean released = slotLockService.release(businessId, staffId, startTime, lockToken);
        assertTrue(released);
    }

    @Test
    @DisplayName("release: Token eşleşmediğinde (0) kilit silinmez ve false döner")
    void releaseTokenMismatch() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(), eq(lockToken))).thenReturn(0L);

        boolean released = slotLockService.release(businessId, staffId, startTime, lockToken);
        assertFalse(released);
    }
}
