package com.clothingbrand.ecommerce.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityService {

    private final MeterRegistry registry;

    // Login counters
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;

    // Rate limit counter
    private final Counter rateLimitRejectionCounter;

    // Email verification counters
    private final Counter verificationRequestCounter;
    private final Counter verificationSuccessCounter;
    private final Counter verificationFailureCounter;

    // Password reset counters
    private final Counter resetRequestCounter;
    private final Counter resetSuccessCounter;
    private final Counter resetFailureCounter;

    // Stripe reservation counters
    private final Counter reservationCreateCounter;
    private final Counter reservationReuseCounter;
    private final Counter reservationFailureCounter;

    // Stripe Webhook counters
    private final Counter webhookVerifiedCounter;
    private final Counter webhookRejectedCounter;
    private final Counter webhookHandledCounter;
    private final Counter webhookFailedCounter;

    // Expiry recovery counter
    private final Counter reservationExpiryRecoveryCounter;

    // Image Upload counters
    private final Counter uploadSuccessCounter;
    private final Counter uploadFailureCounter;

    // Migration counters
    private final Counter migrationDryRunCounter;
    private final Counter migrationWriteSuccessCounter;
    private final Counter migrationWriteFailureCounter;

    public ObservabilityService(MeterRegistry registry) {
        this.registry = registry;

        this.loginSuccessCounter = Counter.builder("app.auth.login")
                .tag("status", "success")
                .description("Successful login operations")
                .register(registry);

        this.loginFailureCounter = Counter.builder("app.auth.login")
                .tag("status", "failure")
                .description("Failed login operations")
                .register(registry);

        this.rateLimitRejectionCounter = Counter.builder("app.auth.ratelimit.rejection")
                .description("Rate limit rejection events")
                .register(registry);

        this.verificationRequestCounter = Counter.builder("app.email.verification")
                .tag("action", "request")
                .description("Email verification token requests")
                .register(registry);

        this.verificationSuccessCounter = Counter.builder("app.email.verification")
                .tag("action", "success")
                .description("Successful email verifications")
                .register(registry);

        this.verificationFailureCounter = Counter.builder("app.email.verification")
                .tag("action", "failure")
                .description("Failed email verifications")
                .register(registry);

        this.resetRequestCounter = Counter.builder("app.password.reset")
                .tag("action", "request")
                .description("Password reset requests")
                .register(registry);

        this.resetSuccessCounter = Counter.builder("app.password.reset")
                .tag("action", "success")
                .description("Successful password resets")
                .register(registry);

        this.resetFailureCounter = Counter.builder("app.password.reset")
                .tag("action", "failure")
                .description("Failed password resets")
                .register(registry);

        this.reservationCreateCounter = Counter.builder("app.payment.reservation")
                .tag("action", "create")
                .description("Stripe payment reservations created")
                .register(registry);

        this.reservationReuseCounter = Counter.builder("app.payment.reservation")
                .tag("action", "reuse")
                .description("Stripe payment reservations reused")
                .register(registry);

        this.reservationFailureCounter = Counter.builder("app.payment.reservation")
                .tag("action", "failure")
                .description("Stripe payment reservation failures")
                .register(registry);

        this.webhookVerifiedCounter = Counter.builder("app.payment.webhook")
                .tag("status", "verified")
                .description("Stripe webhook verification success")
                .register(registry);

        this.webhookRejectedCounter = Counter.builder("app.payment.webhook")
                .tag("status", "rejected")
                .description("Stripe webhook verification failure")
                .register(registry);

        this.webhookHandledCounter = Counter.builder("app.payment.webhook")
                .tag("status", "handled")
                .description("Stripe webhook handled successfully")
                .register(registry);

        this.webhookFailedCounter = Counter.builder("app.payment.webhook")
                .tag("status", "failed")
                .description("Stripe webhook handling failure")
                .register(registry);

        this.reservationExpiryRecoveryCounter = Counter.builder("app.payment.expiry.recovery")
                .description("Expired reservation cleanup and stock restorations")
                .register(registry);

        this.uploadSuccessCounter = Counter.builder("app.storage.upload")
                .tag("status", "success")
                .description("Successful image uploads")
                .register(registry);

        this.uploadFailureCounter = Counter.builder("app.storage.upload")
                .tag("status", "failure")
                .description("Failed image uploads")
                .register(registry);

        this.migrationDryRunCounter = Counter.builder("app.storage.migration")
                .tag("mode", "dryrun")
                .description("Migration dry-run executions")
                .register(registry);

        this.migrationWriteSuccessCounter = Counter.builder("app.storage.migration")
                .tag("mode", "write_success")
                .description("Successful migration write runs")
                .register(registry);

        this.migrationWriteFailureCounter = Counter.builder("app.storage.migration")
                .tag("mode", "write_failure")
                .description("Failed migration write runs")
                .register(registry);
    }

    public void trackLoginSuccess() { loginSuccessCounter.increment(); }
    public void trackLoginFailure() { loginFailureCounter.increment(); }
    public void trackRateLimitRejection() { rateLimitRejectionCounter.increment(); }
    public void trackVerificationRequest() { verificationRequestCounter.increment(); }
    public void trackVerificationSuccess() { verificationSuccessCounter.increment(); }
    public void trackVerificationFailure() { verificationFailureCounter.increment(); }
    public void trackResetRequest() { resetRequestCounter.increment(); }
    public void trackResetSuccess() { resetSuccessCounter.increment(); }
    public void trackResetFailure() { resetFailureCounter.increment(); }
    public void trackReservationCreate() { reservationCreateCounter.increment(); }
    public void trackReservationReuse() { reservationReuseCounter.increment(); }
    public void trackReservationFailure() { reservationFailureCounter.increment(); }
    public void trackWebhookVerified() { webhookVerifiedCounter.increment(); }
    public void trackWebhookRejected() { webhookRejectedCounter.increment(); }
    public void trackWebhookHandled() { webhookHandledCounter.increment(); }
    public void trackWebhookFailed() { webhookFailedCounter.increment(); }
    public void trackReservationExpiryRecovery() { reservationExpiryRecoveryCounter.increment(); }
    public void trackUploadSuccess() { uploadSuccessCounter.increment(); }
    public void trackUploadFailure() { uploadFailureCounter.increment(); }
    public void trackMigrationDryRun() { migrationDryRunCounter.increment(); }
    public void trackMigrationWriteSuccess() { migrationWriteSuccessCounter.increment(); }
    public void trackMigrationWriteFailure() { migrationWriteFailureCounter.increment(); }
}
