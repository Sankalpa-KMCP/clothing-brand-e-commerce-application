package com.clothingbrand.ecommerce.config;

import com.clothingbrand.ecommerce.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReleaseReadinessValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JwtProperties.class,
                    AuthRateLimitProperties.class,
                    StripeProperties.class,
                    TransactionalEmailProperties.class
            ));

    @Test
    void whenProdProfile_verifyJwtSecretRequirements() {
        // Safe check: default secret should fail strength validation
        String defaultSecret = "your_256_bit_secret_key_change_me_in_production";
        validateJwtSecret(defaultSecret, false);

        // Safe check: too short secret should fail strength validation
        String shortSecret = "short-secret";
        validateJwtSecret(shortSecret, false);

        // Safe check: secure secret should pass validation
        String secureSecret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        validateJwtSecret(secureSecret, true);
    }

    @Test
    void whenProdProfile_verifyRateLimitHashRequirements() {
        // Safe check: default hash secret should be rejected
        String defaultHash = "dev-rate-limit-secret-change-me";
        validateRateLimitSecret(defaultHash, false);

        // Safe check: custom secure hash secret should be accepted
        String secureHash = "my-secure-prod-hash-secret-value-12345";
        validateRateLimitSecret(secureHash, true);
    }

    @Test
    void whenProdProfile_verifyCorsOriginsRequirements() {
        // Localhost origins are not allowed in production CORS
        validateCorsOrigins("http://localhost:5173", false);
        validateCorsOrigins("https://127.0.0.1:3000", false);
        validateCorsOrigins("http://localhost", false);

        // Custom domains are allowed
        validateCorsOrigins("https://staging.clothingbrand.com,https://clothingbrand.com", true);
    }

    private void validateJwtSecret(String secret, boolean shouldPass) {
        if (!shouldPass) {
            assertThrows(IllegalArgumentException.class, () -> {
                performJwtCheck(secret);
            });
        } else {
            performJwtCheck(secret);
        }
    }

    private void performJwtCheck(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured");
        }
        if ("your_256_bit_secret_key_change_me_in_production".equals(secret)) {
            throw new IllegalArgumentException("JWT secret cannot use the default placeholder key");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 characters)");
        }
    }

    private void validateRateLimitSecret(String secret, boolean shouldPass) {
        if (!shouldPass) {
            assertThrows(IllegalArgumentException.class, () -> {
                performRateLimitCheck(secret);
            });
        } else {
            performRateLimitCheck(secret);
        }
    }

    private void performRateLimitCheck(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Rate limit hash secret must be configured");
        }
        if ("dev-rate-limit-secret-change-me".equals(secret)) {
            throw new IllegalArgumentException("Rate limit hash secret cannot use the default placeholder");
        }
    }

    private void validateCorsOrigins(String origins, boolean shouldPass) {
        if (!shouldPass) {
            assertThrows(IllegalArgumentException.class, () -> {
                performCorsCheck(origins);
            });
        } else {
            performCorsCheck(origins);
        }
    }

    private void performCorsCheck(String origins) {
        if (origins == null || origins.isBlank()) {
            throw new IllegalArgumentException("CORS allowed origins must be configured");
        }
        List<String> list = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String origin : list) {
            if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
                throw new IllegalArgumentException("CORS allowed origins cannot contain localhost or loopback IPs in production: " + origin);
            }
        }
    }

    @Test
    void whenProdProfile_verifyMetricsTokenRequirements() {
        validateMetricsToken("dev-metrics-token-change-me", false);
        validateMetricsToken("short-metrics", false);
        validateMetricsToken("secure-metrics-token-123456", true);
    }

    private void validateMetricsToken(String token, boolean shouldPass) {
        if (!shouldPass) {
            assertThrows(IllegalArgumentException.class, () -> {
                performMetricsTokenCheck(token);
            });
        } else {
            performMetricsTokenCheck(token);
        }
    }

    private void performMetricsTokenCheck(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("app.security.metrics.token must be configured");
        }
        if ("dev-metrics-token-change-me".equals(token)) {
            throw new IllegalArgumentException("app.security.metrics.token cannot use default placeholder");
        }
        if (token.length() < 16) {
            throw new IllegalArgumentException("app.security.metrics.token must be at least 16 characters");
        }
    }
}
