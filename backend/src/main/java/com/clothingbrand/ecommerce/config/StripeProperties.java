package com.clothingbrand.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.payment.stripe")
public class StripeProperties {
    private boolean enabled = false;
    private String secretKey;
    private String webhookSecret;
    private String currency = "USD";
    private String redirectBaseUrl;
    private int sessionTimeoutSeconds = 1800;

    public int getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(int sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    public void setRedirectBaseUrl(String redirectBaseUrl) {
        this.redirectBaseUrl = redirectBaseUrl;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment env;

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (enabled) {
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException("app.payment.stripe.secret-key must be configured when Stripe is enabled");
            }
            if (webhookSecret == null || webhookSecret.isBlank()) {
                throw new IllegalStateException("app.payment.stripe.webhook-secret must be configured when Stripe is enabled");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalStateException("app.payment.stripe.currency must be configured when Stripe is enabled");
            }
            if (redirectBaseUrl == null || redirectBaseUrl.isBlank()) {
                throw new IllegalStateException("app.payment.stripe.redirect-base-url must be configured when Stripe is enabled");
            }
            if (env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("prod")) {
                if ("your_stripe_secret_key".equals(secretKey)) {
                    throw new IllegalStateException("app.payment.stripe.secret-key cannot use default placeholder in production profile");
                }
                if ("your_stripe_webhook_secret".equals(webhookSecret)) {
                    throw new IllegalStateException("app.payment.stripe.webhook-secret cannot use default placeholder in production profile");
                }
            }
        }
    }
}
