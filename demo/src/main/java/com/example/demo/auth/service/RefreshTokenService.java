package com.example.demo.auth.service;

import com.example.demo.auth.dto.AuthResponse;
import com.example.demo.auth.dto.UserSummaryDto;
import com.example.demo.auth.exception.TokenRefreshException;
import com.example.demo.common.util.HashUtils;
import com.example.demo.security.config.JwtProperties;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.user.entity.RefreshToken;
import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserStatus;
import com.example.demo.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates and persists a new RefreshToken in the database (stored as SHA-256 hash).
     *
     * @param user      Owner of the refresh token
     * @param ipAddress IP address of the client
     * @return Raw unhashed token string to send to the client
     */
    @Transactional
    public String createRefreshToken(User user, String ipAddress) {
        String rawToken = generateSecureRandomToken();
        String tokenHash = HashUtils.sha256Hex(rawToken);

        Instant expiresAt = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Verifies the provided refresh token, checks for reuse / expiration,
     * invalidates the old token, and generates a new access & refresh token pair (Rotation).
     *
     * @param rawRefreshToken Raw refresh token from request
     * @param ipAddress       Client IP address
     * @return New AuthResponse containing rotated tokens
     */
    @Transactional
    public AuthResponse verifyAndRotateRefreshToken(String rawRefreshToken, String ipAddress) {
        String tokenHash = HashUtils.sha256Hex(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid"));

        User user = storedToken.getUser();

        // 1. Check for Token Reuse (Breach Detection)
        if (storedToken.getRevokedAt() != null || storedToken.getReplacedByToken() != null) {
            log.warn("Security Alert: Refresh token reuse attempt detected for user: {}. Revoking all active tokens.", user.getUsername());
            // Invalidate all tokens for this user immediately
            refreshTokenRepository.revokeAllActiveTokensForUser(user, Instant.now());
            throw new TokenRefreshException("Suspicious activity: Refresh token reuse detected. All sessions terminated.");
        }

        // 2. Check for Expiration
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }

        // 3. Check User Account Status
        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new TokenRefreshException("User account is inactive, locked, or deleted.");
        }

        // 4. Token Rotation: Issue new refresh token
        String newRawRefreshToken = generateSecureRandomToken();
        String newTokenHash = HashUtils.sha256Hex(newRawRefreshToken);
        Instant now = Instant.now();

        // Mark the previous token as revoked and record its replacement
        storedToken.setRevokedAt(now);
        storedToken.setReplacedByToken(newTokenHash);
        refreshTokenRepository.save(storedToken);

        // Save the new refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(now.plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .ipAddress(ipAddress)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // 5. Generate new Access Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
                .user(UserSummaryDto.fromEntity(user))
                .build();
    }

    /**
     * Revokes a refresh token upon logout.
     *
     * @param rawRefreshToken Raw refresh token string
     */
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = HashUtils.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    /**
     * Revokes all active refresh tokens for a user (e.g., password change, force logout).
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllActiveTokensForUser(user, Instant.now());
    }

    /**
     * Generates a cryptographically strong, URL-safe random string.
     */
    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) + "-" + UUID.randomUUID();
    }
}
