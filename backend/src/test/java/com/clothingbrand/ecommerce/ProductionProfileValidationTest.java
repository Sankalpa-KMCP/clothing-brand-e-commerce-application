package com.clothingbrand.ecommerce;

import com.clothingbrand.ecommerce.security.JwtProperties;
import com.clothingbrand.ecommerce.config.AuthRateLimitProperties;
import com.clothingbrand.ecommerce.config.StripeProperties;
import com.clothingbrand.ecommerce.config.MetricsSecurityProperties;
import com.clothingbrand.ecommerce.storage.S3StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionProfileValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(
                    TestConfig.class,
                    PropertyPlaceholderAutoConfiguration.class
            )
            .withPropertyValues(
                    "spring.profiles.active=prod",
                    "DB_HOST=localhost",
                    "DB_PORT=5432",
                    "DB_NAME=ecommerce_db",
                    "DB_USERNAME=postgres",
                    "DB_PASSWORD=password",
                    "JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    "app.auth-rate-limit.hash-secret=secure_hash_secret_value_123456",
                    "app.security.metrics.token=secure_metrics_token_value_123456"
            );

    @Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties({
            JwtProperties.class,
            AuthRateLimitProperties.class,
            StripeProperties.class,
            MetricsSecurityProperties.class,
            S3StorageProperties.class
    })
    static class TestConfig {
        @Value("${app.cors.allowed-origins}")
        private String allowedOrigins;

        @Value("${app.upload.dir}")
        private String uploadDir;

        @Value("${app.upload.base-url}")
        private String uploadBaseUrl;
    }

    @Test
    void testProdProfile_failsWhenCorsAndUploadPropertiesAreMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            Throwable startupFailure = context.getStartupFailure();
            assertThat(startupFailure).isNotNull();
            
            Throwable rootCause = startupFailure;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage())
                    .contains("Could not resolve placeholder");
        });
    }

    @Test
    void testProdProfile_succeedsWhenAllPropertiesAreProvided() {
        contextRunner.withPropertyValues(
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("app.cors.allowed-origins")).isEqualTo("https://example.com");
        });
    }

    @Test
    void testProdProfile_failsWhenJwtSecretIsDefault() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=your_256_bit_secret_key_change_me_in_production",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("JWT secret cannot use default placeholder key");
        });
    }

    @Test
    void testProdProfile_failsWhenJwtSecretIsTooShort() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=too-short",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("JWT secret must be at least 256 bits");
        });
    }

    @Test
    void testProdProfile_failsWhenRateLimitHashSecretIsDefault() {
        contextRunner.withPropertyValues(
                "app.auth-rate-limit.hash-secret=dev-rate-limit-secret-change-me",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("hash-secret cannot use default placeholder");
        });
    }

    @Test
    void testProdProfile_failsWhenMetricsTokenIsDefault() {
        contextRunner.withPropertyValues(
                "app.security.metrics.token=dev-metrics-token-change-me",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("token cannot use default placeholder");
        });
    }

    @Test
    void testProdProfile_failsWhenStripeSecretIsDefault() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true",
                "app.payment.stripe.secret-key=your_stripe_secret_key",
                "app.payment.stripe.webhook-secret=secure_webhook_secret",
                "app.payment.stripe.currency=USD",
                "app.payment.stripe.redirect-base-url=https://example.com",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("secret-key cannot use default placeholder");
        });
    }

    @Test
    void testProdProfile_failsWhenS3AccessKeyIsDefault() {
        contextRunner.withPropertyValues(
                "app.storage.type=s3",
                "app.storage.s3.endpoint=https://example.r2.cloudflarestorage.com",
                "app.storage.s3.region=auto",
                "app.storage.s3.bucket-name=my-bucket",
                "app.storage.s3.access-key=your_s3_access_key",
                "app.storage.s3.secret-key=secure_s3_secret",
                "app.storage.s3.public-url-prefix=https://cdn.example.com/",
                "CORS_ALLOWED_ORIGINS=https://example.com",
                "UPLOAD_DIR=/app/uploads",
                "UPLOAD_BASE_URL=https://example.com/api/images/"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable rootCause = context.getStartupFailure();
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            assertThat(rootCause.getMessage()).contains("access-key cannot use default placeholder");
        });
    }
}
