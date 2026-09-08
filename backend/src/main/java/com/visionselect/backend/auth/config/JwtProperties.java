package com.visionselect.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.security.jwt.*} from application.yml.
 * Values come from environment variables via the YAML placeholders.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMinutes,
        long refreshTokenExpirationDays
) {
}
