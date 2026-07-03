package com.clothingbrand.ecommerce;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class StagingConfigurationValidationTest {

    private File locateFile(String path) {
        File f = new File(path);
        if (f.exists()) {
            return f;
        }
        f = new File("../" + path);
        if (f.exists()) {
            return f;
        }
        return null;
    }

    @Test
    public void testStagingEnvTemplate_isSafeAndCorrect() throws Exception {
        File envFile = locateFile(".env.staging.example");
        assertThat(envFile).describedAs("env.staging.example file should exist").isNotNull();

        List<String> lines = Files.readAllLines(envFile.toPath());
        
        boolean hasJwtSecret = false;
        boolean hasDbPassword = false;
        boolean hasStripeEnabled = false;
        boolean hasEmailEnabled = false;
        boolean hasMigrationEnabled = false;
        boolean hasMetricsToken = false;
        boolean hasCorsLocalhost = false;
        boolean hasTrustForwarded = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            if (line.startsWith("JWT_SECRET=")) {
                hasJwtSecret = true;
                String val = line.substring("JWT_SECRET=".length());
                assertThat(val).contains("change_me");
                assertThat(val.length()).isGreaterThanOrEqualTo(32); // Must meet size checks
            }
            if (line.startsWith("DB_PASSWORD=")) {
                hasDbPassword = true;
                String val = line.substring("DB_PASSWORD=".length());
                assertThat(val).contains("change_me");
            }
            if (line.startsWith("STRIPE_ENABLED=")) {
                hasStripeEnabled = true;
                assertThat(line).isEqualTo("STRIPE_ENABLED=false");
            }
            if (line.startsWith("EMAIL_ENABLED=")) {
                hasEmailEnabled = true;
                assertThat(line).isEqualTo("EMAIL_ENABLED=false");
            }
            if (line.startsWith("MIGRATION_LOCAL_TO_S3_ENABLED=")) {
                hasMigrationEnabled = true;
                assertThat(line).isEqualTo("MIGRATION_LOCAL_TO_S3_ENABLED=false");
            }
            if (line.startsWith("METRICS_SECURITY_TOKEN=")) {
                hasMetricsToken = true;
                String val = line.substring("METRICS_SECURITY_TOKEN=".length());
                assertThat(val).contains("change_me");
                assertThat(val.length()).isGreaterThanOrEqualTo(16); // Must meet size checks
            }
            if (line.startsWith("CORS_ALLOWED_ORIGINS=")) {
                String val = line.substring("CORS_ALLOWED_ORIGINS=".length());
                if (val.contains("localhost") || val.contains("127.0.0.1")) {
                    hasCorsLocalhost = true;
                }
            }
            if (line.startsWith("AUTH_RATE_LIMIT_TRUST_FORWARDED_FOR=")) {
                hasTrustForwarded = true;
                assertThat(line).isEqualTo("AUTH_RATE_LIMIT_TRUST_FORWARDED_FOR=true");
            }
        }

        assertThat(hasJwtSecret).isTrue();
        assertThat(hasDbPassword).isTrue();
        assertThat(hasStripeEnabled).isTrue();
        assertThat(hasEmailEnabled).isTrue();
        assertThat(hasMigrationEnabled).isTrue();
        assertThat(hasMetricsToken).isTrue();
        assertThat(hasTrustForwarded).isTrue();
        assertThat(hasCorsLocalhost).describedAs("CORS allowed origins in production active profiles cannot contain localhost").isFalse();
    }

    @Test
    public void testComposeStaging_isSecure() throws Exception {
        File composeFile = locateFile("compose.staging.yaml");
        assertThat(composeFile).describedAs("compose.staging.yaml should exist").isNotNull();

        String content = Files.readString(composeFile.toPath());

        // Validate that backend port 8080 or DB port 5432 are NOT host-published.
        // Nginx is the only service that should publish ports (e.g., "${STAGING_HTTP_PORT:-80}:80").
        // We ensure ports section is only declared for frontend.
        
        // Find positions of services
        int idxPostgres = content.indexOf("postgres-staging:");
        int idxMigration = content.indexOf("db-migration:");
        int idxBackend = content.indexOf("backend:");
        int idxFrontend = content.indexOf("frontend:");

        assertThat(idxPostgres).isNotEqualTo(-1);
        assertThat(idxMigration).isNotEqualTo(-1);
        assertThat(idxBackend).isNotEqualTo(-1);
        assertThat(idxFrontend).isNotEqualTo(-1);

        // Check ports block in backend service: should not exist between backend: and frontend:
        String backendConfig = content.substring(idxBackend, idxFrontend);
        assertThat(backendConfig).doesNotContain("ports:");

        // Check ports block in postgres service: should not exist between postgres-staging: and db-migration:
        String postgresConfig = content.substring(idxPostgres, idxMigration);
        postgresConfig = postgresConfig.replaceAll("ports:\\s*$", ""); // Strip dangling if any
        assertThat(postgresConfig).doesNotContain("ports:");

        // Check that db-migration has local-to-S3 migration disabled
        String migrationConfig = content.substring(idxMigration, idxBackend);
        assertThat(migrationConfig).contains("MIGRATION_LOCAL_TO_S3_ENABLED: \"false\"");
        assertThat(migrationConfig).contains("SPRING_MAIN_WEB_APPLICATION_TYPE: \"none\"");

        // Check that backend has trust-forwarded-for enabled
        assertThat(backendConfig).contains("app.auth-rate-limit.trust-forwarded-for: \"true\"");
    }

    @Test
    public void testNginxStaging_isHardened() throws Exception {
        File nginxFile = locateFile("frontend/nginx.staging.conf");
        assertThat(nginxFile).describedAs("nginx.staging.conf should exist").isNotNull();

        String content = Files.readString(nginxFile.toPath());

        // Check public actuator endpoints (health, info) allowed, and rest blocked
        assertThat(content).contains("location ~ ^/actuator/(health|info)");
        assertThat(content).contains("location /actuator");
        assertThat(content).contains("deny all;");
        assertThat(content).contains("return 403;");

        // Check Stripe webhook is specifically routed
        assertThat(content).contains("location = /api/webhooks/stripe");
        assertThat(content).contains("location /api/webhooks/");

        // Check client forwarding headers are overwritten
        assertThat(content).contains("proxy_set_header X-Forwarded-For $remote_addr;");
        assertThat(content).contains("proxy_set_header X-Forwarded-Proto $scheme;");
        assertThat(content).contains("proxy_set_header X-Forwarded-Host $host;");
        
        // Check clickjacking protection frame header
        assertThat(content).contains("add_header X-Frame-Options \"DENY\" always;");
        
        // Check CSP is defined
        assertThat(content).contains("add_header Content-Security-Policy");
    }
}
