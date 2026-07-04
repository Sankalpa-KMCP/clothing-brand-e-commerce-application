package com.clothingbrand.ecommerce.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@ConfigurationProperties(prefix = "app.security.metrics")
public class MetricsSecurityProperties {

    private String token = "dev-metrics-token-change-me";

    @Autowired
    private Environment env;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @PostConstruct
    public void validate() {
        if (env != null && Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("app.security.metrics.token must be configured in production profile");
            }
            if ("dev-metrics-token-change-me".equals(token)) {
                throw new IllegalStateException("app.security.metrics.token cannot use default placeholder in production profile");
            }
            if (token.length() < 16) {
                throw new IllegalStateException("app.security.metrics.token must be at least 16 characters in production profile");
            }
        }
    }
}
