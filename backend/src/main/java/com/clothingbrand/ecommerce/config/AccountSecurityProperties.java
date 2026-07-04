package com.clothingbrand.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.account")
public class AccountSecurityProperties {

    private String frontendBaseUrl = "http://localhost:5173";
    private final EmailVerification emailVerification = new EmailVerification();
    private final PasswordReset passwordReset = new PasswordReset();

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public EmailVerification getEmailVerification() {
        return emailVerification;
    }

    public PasswordReset getPasswordReset() {
        return passwordReset;
    }

    public static class EmailVerification {
        private boolean enabled = true;
        private boolean required = false;
        private long tokenExpirationMinutes = 60;
        private boolean legacyUsersExempt = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public long getTokenExpirationMinutes() { return tokenExpirationMinutes; }
        public void setTokenExpirationMinutes(long tokenExpirationMinutes) { this.tokenExpirationMinutes = tokenExpirationMinutes; }
        public boolean isLegacyUsersExempt() { return legacyUsersExempt; }
        public void setLegacyUsersExempt(boolean legacyUsersExempt) { this.legacyUsersExempt = legacyUsersExempt; }
    }

    public static class PasswordReset {
        private boolean enabled = true;
        private long tokenExpirationMinutes = 30;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getTokenExpirationMinutes() { return tokenExpirationMinutes; }
        public void setTokenExpirationMinutes(long tokenExpirationMinutes) { this.tokenExpirationMinutes = tokenExpirationMinutes; }
    }
}
