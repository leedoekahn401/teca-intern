package com.example.demo.security.jwt;

import com.example.demo.security.config.JwtProperties;
import com.example.demo.security.user.CustomUserDetails;
import com.example.demo.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = getSecretKeyFromProperties();
    }

    private SecretKey getSecretKeyFromProperties() {
        String secret = jwtProperties.getSecretKey();
        byte[] keyBytes;
        try {
            // Attempt hex decoding first (Java 17 HexFormat)
            keyBytes = java.util.HexFormat.of().parseHex(secret);
        } catch (Exception e) {
            try {
                // Fallback to base64 decoding
                keyBytes = Decoders.BASE64.decode(secret);
            } catch (Exception ex) {
                // Fallback to raw utf-8 bytes
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed Access Token for an authenticated User entity.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId().toString());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("role", user.getRole().name());

        return buildToken(extraClaims, user.getUsername(), jwtProperties.getAccessTokenExpirationMs());
    }

    /**
     * Generates a signed Access Token from Spring Security Authentication.
     */
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userPrincipal.getId().toString());
        extraClaims.put("email", userPrincipal.getEmail());
        extraClaims.put("role", userPrincipal.getRole().name());

        return buildToken(extraClaims, userPrincipal.getUsername(), jwtProperties.getAccessTokenExpirationMs());
    }

    /**
     * Builds JWT string with claims, subject, issuedAt, expiration and signs with HMAC-SHA256.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts username (subject) from JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts user ID from JWT claims.
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Extracts role from JWT claims.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts expiration date from JWT.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a single claim using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and returns all claims from the JWT.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates if the token is valid, correctly signed, and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
