package com.clothingbrand.ecommerce.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.auth-rate-limit")
public class AuthRateLimitProperties {

    private boolean enabled = true;
    private String hashSecret = "dev-rate-limit-secret-change-me";
    private boolean trustForwardedFor = false;
    private Map<String, Rule> rules = defaultRules();

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment env;

    @PostConstruct
    public void validate() {
        if (enabled && (hashSecret == null || hashSecret.isBlank())) {
            throw new IllegalStateException("app.auth-rate-limit.hash-secret must be configured when auth rate limiting is enabled");
        }
        if (enabled && env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            if ("dev-rate-limit-secret-change-me".equals(hashSecret)) {
                throw new IllegalStateException("app.auth-rate-limit.hash-secret cannot use default placeholder in production profile");
            }
        }
    }

    private Map<String, Rule> defaultRules() {
        Map<String, Rule> defaults = new HashMap<>();
        defaults.put("register", new Rule(20, 3600));
        defaults.put("login", new Rule(30, 900));
        defaults.put("refresh", new Rule(60, 900));
        defaults.put("resend-verification", new Rule(5, 3600));
        defaults.put("forgot-password", new Rule(5, 3600));
        defaults.put("reset-password", new Rule(10, 900));
        return defaults;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }
    public boolean isTrustForwardedFor() { return trustForwardedFor; }
    public void setTrustForwardedFor(boolean trustForwardedFor) { this.trustForwardedFor = trustForwardedFor; }
    public Map<String, Rule> getRules() { return rules; }
    public void setRules(Map<String, Rule> rules) { this.rules = rules; }

    public static class Rule {
        private int limit;
        private long windowSeconds;

        public Rule() {}

        public Rule(int limit, long windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public long getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }
    }
}
