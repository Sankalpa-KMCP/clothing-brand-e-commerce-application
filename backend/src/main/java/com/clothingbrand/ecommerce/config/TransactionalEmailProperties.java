package com.clothingbrand.ecommerce.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.email")
public class TransactionalEmailProperties {

    private boolean enabled = false;
    private String mode = "fake";
    private String fromAddress = "noreply@example.com";
    private final Smtp smtp = new Smtp();

    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        if ("smtp".equalsIgnoreCase(mode)) {
            if (isBlank(fromAddress) || isBlank(smtp.host) || smtp.port <= 0 || isBlank(smtp.username) || isBlank(smtp.password)) {
                throw new IllegalStateException("SMTP email is enabled but EMAIL_FROM_ADDRESS, EMAIL_SMTP_HOST, EMAIL_SMTP_PORT, EMAIL_SMTP_USERNAME, and EMAIL_SMTP_PASSWORD must be configured");
            }
        } else if (!"fake".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("Unsupported email mode: " + mode);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public Smtp getSmtp() { return smtp; }

    public static class Smtp {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private boolean startTls = true;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public boolean isStartTls() { return startTls; }
        public void setStartTls(boolean startTls) { this.startTls = startTls; }
    }
}
