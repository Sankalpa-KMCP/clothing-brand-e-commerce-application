package com.clothingbrand.ecommerce.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for ObservabilityService metric counters.
 * No Spring context or database required.
 */
class ObservabilityServiceTest {

    private MeterRegistry meterRegistry;
    private ObservabilityService observabilityService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        observabilityService = new ObservabilityService(meterRegistry);
    }

    @Test
    void testLoginSuccessIncrement() {
        double initial = getCounterValue("app.auth.login", "status", "success");
        observabilityService.trackLoginSuccess();
        assertThat(getCounterValue("app.auth.login", "status", "success")).isEqualTo(initial + 1.0);
    }

    @Test
    void testLoginFailureIncrement() {
        double initial = getCounterValue("app.auth.login", "status", "failure");
        observabilityService.trackLoginFailure();
        assertThat(getCounterValue("app.auth.login", "status", "failure")).isEqualTo(initial + 1.0);
    }

    @Test
    void testRateLimitRejectionIncrement() {
        double initial = getCounterValue("app.auth.ratelimit.rejection", null, null);
        observabilityService.trackRateLimitRejection();
        assertThat(getCounterValue("app.auth.ratelimit.rejection", null, null)).isEqualTo(initial + 1.0);
    }

    @Test
    void testVerificationRequestIncrement() {
        double initial = getCounterValue("app.email.verification", "action", "request");
        observabilityService.trackVerificationRequest();
        assertThat(getCounterValue("app.email.verification", "action", "request")).isEqualTo(initial + 1.0);
    }

    @Test
    void testVerificationSuccessIncrement() {
        double initial = getCounterValue("app.email.verification", "action", "success");
        observabilityService.trackVerificationSuccess();
        assertThat(getCounterValue("app.email.verification", "action", "success")).isEqualTo(initial + 1.0);
    }

    @Test
    void testVerificationFailureIncrement() {
        double initial = getCounterValue("app.email.verification", "action", "failure");
        observabilityService.trackVerificationFailure();
        assertThat(getCounterValue("app.email.verification", "action", "failure")).isEqualTo(initial + 1.0);
    }

    @Test
    void testResetRequestIncrement() {
        double initial = getCounterValue("app.password.reset", "action", "request");
        observabilityService.trackResetRequest();
        assertThat(getCounterValue("app.password.reset", "action", "request")).isEqualTo(initial + 1.0);
    }

    @Test
    void testUploadSuccessIncrement() {
        double initial = getCounterValue("app.storage.upload", "status", "success");
        observabilityService.trackUploadSuccess();
        assertThat(getCounterValue("app.storage.upload", "status", "success")).isEqualTo(initial + 1.0);
    }

    @Test
    void testMigrationDryRunIncrement() {
        double initial = getCounterValue("app.storage.migration", "mode", "dryrun");
        observabilityService.trackMigrationDryRun();
        assertThat(getCounterValue("app.storage.migration", "mode", "dryrun")).isEqualTo(initial + 1.0);
    }

    @Test
    void testWebhookVerifiedIncrement() {
        double initial = getCounterValue("app.payment.webhook", "status", "verified");
        observabilityService.trackWebhookVerified();
        assertThat(getCounterValue("app.payment.webhook", "status", "verified")).isEqualTo(initial + 1.0);
    }

    @Test
    void testReservationCreateIncrement() {
        double initial = getCounterValue("app.payment.reservation", "action", "create");
        observabilityService.trackReservationCreate();
        assertThat(getCounterValue("app.payment.reservation", "action", "create")).isEqualTo(initial + 1.0);
    }

    private double getCounterValue(String name, String tagKey, String tagValue) {
        try {
            if (tagKey == null) {
                return meterRegistry.get(name).counter().count();
            }
            return meterRegistry.get(name).tag(tagKey, tagValue).counter().count();
        } catch (Exception e) {
            return 0.0;
        }
    }
}
