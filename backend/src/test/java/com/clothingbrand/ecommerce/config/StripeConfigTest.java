package com.clothingbrand.ecommerce.config;

import com.clothingbrand.ecommerce.payment.StripeGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;

class StripeConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    StripeProperties.class,
                    StripeGateway.class
            ));

    @Test
    void whenStripeDisabled_shouldStartSuccessfullyWithoutSecrets() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=false"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            StripeProperties properties = context.getBean(StripeProperties.class);
            assertThat(properties.isEnabled()).isFalse();

            StripeGateway gateway = context.getBean(StripeGateway.class);
            assertThat(gateway.isEnabled()).isFalse();
            
            // Getting client should throw error
            IllegalStateException ex = assertThrows(IllegalStateException.class, gateway::getStripeClient);
            assertThat(ex.getMessage()).contains("disabled");
        });
    }

    @Test
    void whenStripeEnabledAndSecretsMissing_shouldFailFastOnStartup() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();
            
            // Get root cause
            Throwable rootCause = failure;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            
            assertThat(rootCause).isInstanceOf(IllegalStateException.class);
            // Verify that we do NOT expose any secret values in the exception message, just the name of missing property
            assertThat(rootCause.getMessage()).contains("app.payment.stripe.secret-key must be configured");
            
            // Make sure the error message doesn't contain any secret values
            assertThat(rootCause.getMessage()).doesNotContain("sk_");
        });
    }

    @Test
    void whenStripeEnabledAndWebhookSecretMissing_shouldFailFastOnStartup() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true",
                "app.payment.stripe.secret-key=sk_test_placeholder"
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();
            
            Throwable rootCause = failure;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            
            assertThat(rootCause).isInstanceOf(IllegalStateException.class);
            assertThat(rootCause.getMessage()).contains("app.payment.stripe.webhook-secret must be configured");
        });
    }

    @Test
    void whenStripeEnabledAndCurrencyMissing_shouldFailFastOnStartup() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true",
                "app.payment.stripe.secret-key=sk_test_placeholder",
                "app.payment.stripe.webhook-secret=whsec_placeholder",
                "app.payment.stripe.currency="
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();
            
            Throwable rootCause = failure;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            
            assertThat(rootCause).isInstanceOf(IllegalStateException.class);
            assertThat(rootCause.getMessage()).contains("app.payment.stripe.currency must be configured");
        });
    }

    @Test
    void whenStripeEnabledAndRedirectUrlMissing_shouldFailFastOnStartup() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true",
                "app.payment.stripe.secret-key=sk_test_placeholder",
                "app.payment.stripe.webhook-secret=whsec_placeholder",
                "app.payment.stripe.currency=USD",
                "app.payment.stripe.redirect-base-url="
        ).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            assertThat(failure).isNotNull();
            
            Throwable rootCause = failure;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            
            assertThat(rootCause).isInstanceOf(IllegalStateException.class);
            assertThat(rootCause.getMessage()).contains("app.payment.stripe.redirect-base-url must be configured");
        });
    }

    @Test
    void whenStripeEnabledWithAllRequiredProperties_shouldStartSuccessfullyAndConstructClient() {
        contextRunner.withPropertyValues(
                "app.payment.stripe.enabled=true",
                "app.payment.stripe.secret-key=sk_test_1234567890abcdef",
                "app.payment.stripe.webhook-secret=whsec_abcdef",
                "app.payment.stripe.currency=EUR",
                "app.payment.stripe.redirect-base-url=http://example.com"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            StripeProperties properties = context.getBean(StripeProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getSecretKey()).isEqualTo("sk_test_1234567890abcdef");
            assertThat(properties.getWebhookSecret()).isEqualTo("whsec_abcdef");
            assertThat(properties.getCurrency()).isEqualTo("EUR");
            assertThat(properties.getRedirectBaseUrl()).isEqualTo("http://example.com");

            StripeGateway gateway = context.getBean(StripeGateway.class);
            assertThat(gateway.isEnabled()).isTrue();
            assertThat(gateway.getStripeClient()).isNotNull();
        });
    }
}
