package com.example.demo.auth.service;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.exception.TokenRefreshException;
import com.example.demo.common.util.HashUtils;
import com.example.demo.security.config.JwtProperties;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.user.entity.RefreshToken;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Spy
    private JwtProperties jwtProperties = new JwtProperties();

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtProperties.setRefreshTokenExpirationMs(604800000); // 7 days
        jwtProperties.setAccessTokenExpirationMs(900000);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("test_user")
                .email("test@example.com")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should create refresh token and store SHA-256 hash in database")
    void testCreateRefreshToken() {
        String rawToken = refreshTokenService.createRefreshToken(testUser, "127.0.0.1");

        assertNotNull(rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals(HashUtils.sha256Hex(rawToken), saved.getTokenHash());
        assertNotEquals(rawToken, saved.getTokenHash()); // Hash is stored, NOT plaintext
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should rotate refresh token when valid")
    void testVerifyAndRotateRefreshToken_Success() {
        String rawOldToken = "old-raw-token-12345";
        String oldHash = HashUtils.sha256Hex(rawOldToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(oldHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-jwt-token");

        AuthResponse response = refreshTokenService.verifyAndRotateRefreshToken(rawOldToken, "192.168.1.1");

        assertNotNull(response);
        assertEquals("new-access-jwt-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals(rawOldToken, response.getRefreshToken()); // New rotated refresh token

        // Verify old token was revoked and replaced
        assertNotNull(storedToken.getRevokedAt());
        assertNotNull(storedToken.getReplacedByToken());
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    @DisplayName("Token Reuse Detection: should revoke all user tokens when a revoked token is used")
    void testVerifyAndRotateRefreshToken_ReuseDetection() {
        String rawCompromisedToken = "stolen-raw-token";
        String tokenHash = HashUtils.sha256Hex(rawCompromisedToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revokedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .replacedByToken("some-new-hash")
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        TokenRefreshException ex = assertThrows(TokenRefreshException.class, () ->
                refreshTokenService.verifyAndRotateRefreshToken(rawCompromisedToken, "10.0.0.1")
        );

        assertTrue(ex.getMessage().contains("reuse detected"));

        // Emergency action: all active tokens for this user must be revoked
        verify(refreshTokenRepository).revokeAllActiveTokensForUser(eq(testUser), any(Instant.class));
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void testVerifyAndRotateRefreshToken_Expired() {
        String rawToken = "expired-raw-token";
        String tokenHash = HashUtils.sha256Hex(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        TokenRefreshException ex = assertThrows(TokenRefreshException.class, () ->
                refreshTokenService.verifyAndRotateRefreshToken(rawToken, "127.0.0.1")
        );

        assertTrue(ex.getMessage().contains("expired"));
        verify(refreshTokenRepository, never()).revokeAllActiveTokensForUser(any(), any());
    }
}
