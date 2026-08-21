package com.example.demo.security.jwt;

import com.example.demo.security.config.JwtProperties;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setAccessTokenExpirationMs(60000); // 1 minute
        jwtProperties.setIssuer("test-issuer");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid access token and extract correct claims")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("john_doe")
                .email("john@example.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("john_doe", jwtTokenProvider.extractUsername(token));
        assertEquals(userId, jwtTokenProvider.extractUserId(token));
        assertEquals("CUSTOMER", jwtTokenProvider.extractRole(token));
    }

    @Test
    @DisplayName("Should reject tampered token")
    void testRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("jane_doe")
                .email("jane@example.com")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);
        String tamperedToken = token + "xyz";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        jwtProperties.setAccessTokenExpirationMs(-1000); // Expired immediately
        jwtTokenProvider.init();

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("expired_user")
                .email("expired@example.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
