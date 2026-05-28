package com.yunus.security.jwt;

import com.yunus.security.JwtService;
import com.yunus.security.UserPrincipal;
import com.yunus.user.entity.User;
import com.yunus.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    // Minimum 256-bit (32 bytes) Base64 encoded secret key
    private static final String SECRET_KEY = "ZGV2LW9ubHktc2VjcmV0LWtleS1taW4tMjU2LWJpdC11enVubHVndW5kYS1vbG1hbGktdXJldGltLWljaW4tZGVnaXN0aXJpbG1lbGktMTIzNDU=";

    @BeforeEach
    void setUp() {
        // 15 dk (900000 ms) access token, 7 gün (604800000 ms) refresh token süreleri
        jwtProperties = new JwtProperties(SECRET_KEY, 900000, 604800000);
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhone("5554443322");
        user.setFullName("Test User");
        user.setRole(UserRole.USER);
        user.setIsActive(true);

        UserPrincipal userDetails = new UserPrincipal(user);

        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals(user.getPhone(), username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void shouldGenerateAndExtractRefreshToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPhone("5554443322");
        user.setRole(UserRole.BUSINESS_OWNER);
        user.setIsActive(true);

        UserPrincipal userDetails = new UserPrincipal(user);

        String refreshToken = jwtService.generateRefreshToken(userDetails);
        assertNotNull(refreshToken);

        String username = jwtService.extractUsername(refreshToken);
        assertEquals(user.getPhone(), username);
    }
}
