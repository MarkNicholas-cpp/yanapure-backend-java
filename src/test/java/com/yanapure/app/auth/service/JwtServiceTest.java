package com.yanapure.app.auth.service;

import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set test configuration
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-very-long-and-secure-for-testing-purposes");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryHours", 1);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryDays", 7);

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setName("Test User");
        testUser.setPhone("+14155552671");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
    }

    @Test
    void testGenerateAccessToken() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed
        Claims claims = jwtService.validateAndParseToken(token);
        assertEquals(testUser.getId().toString(), claims.getSubject());
        assertEquals(testUser.getPhone(), claims.get("phone"));
        assertEquals(testUser.getRole().name(), claims.get("role"));
        assertEquals("access", claims.get("tokenType"));
    }

    @Test
    void testGenerateRefreshToken() {
        // When
        String token = jwtService.generateRefreshToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be parsed
        Claims claims = jwtService.validateAndParseToken(token);
        assertEquals(testUser.getId().toString(), claims.getSubject());
        assertEquals(testUser.getId().toString(), claims.get("userId"));
        assertEquals("refresh", claims.get("tokenType"));
    }

    @Test
    void testValidateAndParseToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Claims claims = jwtService.validateAndParseToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(testUser.getId().toString(), claims.getSubject());
    }

    @Test
    void testValidateAndParseTokenWithInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(RuntimeException.class, () -> jwtService.validateAndParseToken(invalidToken));
    }

    @Test
    void testGetUserIdFromToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String userId = jwtService.getUserIdFromToken(token);

        // Then
        assertEquals(testUser.getId().toString(), userId);
    }

    @Test
    void testGetPhoneFromToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String phone = jwtService.getPhoneFromToken(token);

        // Then
        assertEquals(testUser.getPhone(), phone);
    }

    @Test
    void testGetRoleFromToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Role role = jwtService.getRoleFromToken(token);

        // Then
        assertEquals(testUser.getRole(), role);
    }

    @Test
    void testIsTokenExpired() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void testGetTokenExpiration() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Instant expiration = jwtService.getTokenExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
    }

    @Test
    void testIsAccessToken() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When & Then
        assertTrue(jwtService.isAccessToken(accessToken));
        assertFalse(jwtService.isAccessToken(refreshToken));
    }

    @Test
    void testIsRefreshToken() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When & Then
        assertFalse(jwtService.isRefreshToken(accessToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
    }

    @Test
    void testTokenExpirationTimes() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When
        Instant accessExpiration = jwtService.getTokenExpiration(accessToken);
        Instant refreshExpiration = jwtService.getTokenExpiration(refreshToken);

        // Then
        // Access token should expire in about 1 hour
        long accessExpirySeconds = accessExpiration.getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(accessExpirySeconds > 3500 && accessExpirySeconds < 3700); // ~1 hour

        // Refresh token should expire in about 7 days
        long refreshExpirySeconds = refreshExpiration.getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(refreshExpirySeconds > 604700 && refreshExpirySeconds < 605000); // ~7 days
    }
}
