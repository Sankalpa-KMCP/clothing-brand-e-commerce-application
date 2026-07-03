package com.clothingbrand.ecommerce.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment env;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("JWT secret must be configured in production profile");
            }
            if ("your_256_bit_secret_key_change_me_in_production".equals(secret)) {
                throw new IllegalStateException("JWT secret cannot use default placeholder key in production profile");
            }
            if (secret.length() < 32) {
                throw new IllegalStateException("JWT secret must be at least 256 bits (32 characters) in production profile");
            }
        }
    }
}
