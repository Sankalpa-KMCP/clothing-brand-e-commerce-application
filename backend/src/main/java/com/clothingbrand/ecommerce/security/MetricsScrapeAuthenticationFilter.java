package com.clothingbrand.ecommerce.security;

import com.clothingbrand.ecommerce.config.MetricsSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class MetricsScrapeAuthenticationFilter extends OncePerRequestFilter {

    private final MetricsSecurityProperties properties;

    public MetricsScrapeAuthenticationFilter(MetricsSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("/actuator/prometheus".equals(request.getRequestURI())) {
            String token = request.getHeader("X-Metrics-Token");
            String metricsToken = properties.getToken();
            if (token != null && !token.isBlank() && metricsToken != null && !metricsToken.isBlank()) {
                if (safeEquals(token, metricsToken)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            "metrics-scraper",
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_METRICS"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean safeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            // message digest isEqual is only constant-time for equal lengths, so we pad/hash or just use standard isEqual.
            // MessageDigest.isEqual is actually designed to prevent timing attacks.
        }
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
