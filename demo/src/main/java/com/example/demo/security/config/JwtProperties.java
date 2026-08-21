package com.example.demo.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.security.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens (HMAC-SHA256).
     */
    private String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /**
     * Access token expiration in milliseconds (default: 15 minutes).
     */
    private long accessTokenExpirationMs = 900000;

    /**
     * Refresh token expiration in milliseconds (default: 7 days).
     */
    private long refreshTokenExpirationMs = 604800000;

    /**
     * Issuer claim value for JWT.
     */
    private String issuer = "teca-ecommerce";
}
