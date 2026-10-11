package com.yunus.ratelimit;

import com.yunus.ratelimit.annotation.KeyType;
import com.yunus.ratelimit.annotation.RateLimit;
import com.yunus.ratelimit.interceptor.RateLimitInterceptor;
import com.yunus.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JwtService jwtService;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(redisTemplate, jwtService);
    }

    // Dummy controller methods for testing @RateLimit annotation
    static class SampleController {
        @RateLimit(key = "test_endpoint", limit = 5, windowSeconds = 60, keyType = KeyType.IP)
        public void rateLimitedEndpoint() {}

        public void normalEndpoint() {}
    }

    @Test
    @DisplayName("Method without @RateLimit annotation should pass through")
    void preHandle_whenNoAnnotation_shouldReturnTrue() throws Exception {
        Method method = SampleController.class.getMethod("normalEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new SampleController(), method);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Under limit: Lua script returns 1L -> should allow request (200)")
    void preHandle_whenUnderLimit_shouldReturnTrue() throws Exception {
        Method method = SampleController.class.getMethod("rateLimitedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new SampleController(), method);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Limit exceeded: Lua script returns 0L -> should block with HTTP 429")
    void preHandle_whenLimitExceeded_shouldBlockWith429() throws Exception {
        Method method = SampleController.class.getMethod("rateLimitedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new SampleController(), method);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(0L);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("RATE_LIMIT_EXCEEDED"));
    }
}