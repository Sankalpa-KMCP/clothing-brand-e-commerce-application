package com.clothingbrand.ecommerce.config;

import com.clothingbrand.ecommerce.exception.ErrorResponse;
import com.clothingbrand.ecommerce.exception.GlobalExceptionHandler;
import com.clothingbrand.ecommerce.security.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for observability HTTP behavior:
 * actuator endpoint access control, health probe visibility,
 * correlation ID propagation/sanitization, and error response enrichment.
 *
 * Requires a running PostgreSQL instance (same as all other integration tests).
 */
@SpringBootTest(properties = "management.prometheus.metrics.export.enabled=true")
public class ObservabilityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // CorrelationIdFilter must be added before the security filter chain
        // to ensure X-Correlation-ID header is set even on 401/403 responses.
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(correlationIdFilter)
                .apply(springSecurity())
                .build();
    }

    // ─── Actuator access control ───────────────────────────────────────────

    @Test
    void healthEndpoint_publicAccess_returns200WithStatusOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andReturn();

        // Verify response body does not contain sensitive strings
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("jdbc:", "postgres", "password", "secret",
                "stackTrace", "exception", "hikari");
    }

    @Test
    void livenessProbe_publicAccess_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void readinessProbe_publicAccess_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void infoEndpoint_publicAccess_doesNotReturn401() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusEndpoint_anonymousAccess_returns401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Correlation ID propagation ────────────────────────────────────────

    @Test
    void correlationId_generatedWhenAbsent() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        String correlationId = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull().isNotBlank();
        // Should be a valid UUID format
        assertThat(correlationId).matches("^[a-f0-9\\-]{36}$");
    }

    @Test
    void correlationId_preservedWhenSafeValueSupplied() throws Exception {
        String customTraceId = "my-custom-trace-id-12345";
        mockMvc.perform(get("/actuator/health").header("X-Correlation-ID", customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customTraceId));
    }

    @Test
    void correlationId_sanitizedOnInjectionAttempt() throws Exception {
        String dangerousHeader = "<script>alert('xss')</script>";
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", dangerousHeader))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        String returned = result.getResponse().getHeader("X-Correlation-ID");
        assertThat(returned).isNotEqualTo(dangerousHeader);
        // Should have been replaced with a UUID
        assertThat(returned).matches("^[a-f0-9\\-]{36}$");
    }

    @Test
    void correlationId_sanitizedOnOverlengthInput() throws Exception {
        // 65+ chars should be rejected (max 64)
        String longHeader = "a".repeat(65);
        MvcResult result = mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", longHeader))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Correlation-ID")).isNotEqualTo(longHeader);
    }

    // ─── Correlation ID in error responses ──────────────────────────────────

    @Test
    void errorResponse_contains_correlationId_on404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void errorResponse_contains_correlationId_on401() throws Exception {
        mockMvc.perform(get("/api/cart/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ─── Malformed bearer token on public route ─────────────────────────────

    @Test
    void malformedBearerToken_onPublicHealthRoute_returns401() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ─── Health response safety ─────────────────────────────────────────────

    @Test
    void healthResponse_doesNotExposeInternalDetails() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(
                "jdbc:", "HikariPool", "datasource",
                "password", "secret", "credentials",
                "stackTrace", "exception", "NullPointer",
                "org.springframework", "com.clothingbrand"
        );
    }

    // ─── Metrics Scrape Endpoint Access Controls ────────────────────────────

    @Test
    void prometheusEndpoint_validMetricsToken_returns200() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .header("X-Metrics-Token", "dev-metrics-token-change-me"))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusEndpoint_invalidMetricsToken_returns401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .header("X-Metrics-Token", "invalid-metrics-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusToken_cannotAccessAdminOrCustomerAPIs() throws Exception {
        mockMvc.perform(get("/api/admin/probe")
                        .header("X-Metrics-Token", "dev-metrics-token-change-me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/cart/items")
                        .header("X-Metrics-Token", "dev-metrics-token-change-me"))
                .andExpect(status().isUnauthorized());
    }

    // ─── IllegalArgumentException Hardening ──────────────────────────────────

    @Test
    void testIllegalArgumentException_fromApp_returnsSpecificMessage() {
        IllegalArgumentException appEx = new IllegalArgumentException("App validation error message");
        appEx.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.clothingbrand.ecommerce.domain.cart.CartService", "addItem", "CartService.java", 52)
        });

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(appEx);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("App validation error message");
    }

    @Test
    void testIllegalArgumentException_fromLibrary_returnsGenericMessage() {
        IllegalArgumentException libEx = new IllegalArgumentException("Library internal details");
        libEx.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("java.util.UUID", "fromString", "UUID.java", 200),
                new StackTraceElement("com.clothingbrand.ecommerce.domain.catalog.CatalogController", "getProduct", "CatalogController.java", 15)
        });

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(libEx);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid request parameter");
    }

    // ─── Proxy Forwarding Safety ─────────────────────────────────────────────

    @Test
    void rateLimit_doesNotTrustForwardedForByDefault() throws Exception {
        com.clothingbrand.ecommerce.config.AuthRateLimitProperties properties =
                context.getBean(com.clothingbrand.ecommerce.config.AuthRateLimitProperties.class);
        assertThat(properties.isTrustForwardedFor()).isFalse();
    }
}
