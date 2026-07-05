package com.clothingbrand.ecommerce.bootstrap;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import com.clothingbrand.ecommerce.config.AdminBootstrapProperties;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import com.clothingbrand.ecommerce.EcommerceApplication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest
@Transactional
public class AdminBootstrapIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private String generateUniqueEmail() {
        return "admin-bootstrap-test-" + UUID.randomUUID().toString() + "@test.com";
    }

    private AdminBootstrapRunner createRunner(String email, String password, boolean enabled) {
        AdminBootstrapProperties props = new AdminBootstrapProperties();
        props.setEnabled(enabled);
        props.setEmail(email);
        props.setPassword(password);
        return new AdminBootstrapRunner(props, userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void testAdminBootstrapCreatesUser() throws Exception {
        String uniqueEmail = generateUniqueEmail();
        AdminBootstrapRunner testRunner = createRunner(uniqueEmail, "SafePassword123!", true);
        
        testRunner.run(new DefaultApplicationArguments());

        Optional<User> adminOpt = userRepository.findByEmail(uniqueEmail);
        assertTrue(adminOpt.isPresent());

        User admin = adminOpt.get();
        assertEquals("Admin", admin.getFirstName());
        assertEquals("User", admin.getLastName());
        assertEquals(RoleName.ROLE_ADMIN, admin.getRole().getName());
        assertTrue(admin.getActive());
        assertTrue(passwordEncoder.matches("SafePassword123!", admin.getPasswordHash()));
    }

    @Test
    void testAdminBootstrapIsIdempotent() throws Exception {
        String uniqueEmail = generateUniqueEmail();
        AdminBootstrapRunner testRunner = createRunner(uniqueEmail, "SafePassword123!", true);
        
        long initialCount = userRepository.count();
        
        // Run first time
        testRunner.run(new DefaultApplicationArguments());
        assertEquals(initialCount + 1, userRepository.count());

        // Run second time
        testRunner.run(new DefaultApplicationArguments());
        assertEquals(initialCount + 1, userRepository.count()); // No duplicate created
    }

    @Test
    void testAdminBootstrapDoesNotOverwriteCustomer() throws Exception {
        String uniqueEmail = generateUniqueEmail();
        AdminBootstrapRunner testRunner = createRunner(uniqueEmail, "SafePassword123!", true);

        // Create a customer with the admin email
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        User customer = new User();
        customer.setEmail(uniqueEmail);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setPasswordHash(passwordEncoder.encode("CustomerPass!"));
        customer.setRole(customerRole);
        customer.setActive(true);
        userRepository.save(customer);

        // Run bootstrap
        testRunner.run(new DefaultApplicationArguments());

        // Verify customer was not overwritten
        User user = userRepository.findByEmail(uniqueEmail).orElseThrow();
        assertEquals(RoleName.ROLE_CUSTOMER, user.getRole().getName());
        assertEquals("Jane", user.getFirstName());
        assertTrue(passwordEncoder.matches("CustomerPass!", user.getPasswordHash()));
    }

    @Test
    void testBootstrapFailsWhenEnabledButConfigMissing() {
        AdminBootstrapProperties badProps = new AdminBootstrapProperties();
        badProps.setEnabled(true);
        // email and password missing

        AdminBootstrapRunner badRunner = new AdminBootstrapRunner(badProps, userRepository, roleRepository, passwordEncoder);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            badRunner.run(new DefaultApplicationArguments());
        });

        assertTrue(ex.getMessage().contains("missing valid ADMIN_EMAIL or ADMIN_PASSWORD"));
    }

    @Test
    void testDisabledBootstrapDoesNotFailOnMissingConfig() throws Exception {
        AdminBootstrapProperties disabledProps = new AdminBootstrapProperties();
        disabledProps.setEnabled(false);
        // email and password missing

        AdminBootstrapRunner disabledRunner = new AdminBootstrapRunner(disabledProps, userRepository, roleRepository, passwordEncoder);

        // Should just return silently
        assertDoesNotThrow(() -> {
            disabledRunner.run(new DefaultApplicationArguments());
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testRealStartupFailsWhenEnabledButConfigMissing() {
        // 1. Create a known persisted sentinel user with a unique email
        String uniqueEmail = "sentinel-" + UUID.randomUUID().toString() + "@test.com";
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        User sentinel = new User();
        sentinel.setEmail(uniqueEmail);
        sentinel.setFirstName("Sentinel");
        sentinel.setLastName("User");
        sentinel.setPasswordHash(passwordEncoder.encode("SentinelPass123!"));
        sentinel.setRole(customerRole);
        sentinel.setActive(true);
        sentinel = userRepository.saveAndFlush(sentinel);

        long initialCount = userRepository.count();
        Long sentinelId = sentinel.getId();
        String sentinelHash = sentinel.getPasswordHash();

        Exception startupException = null;
        ConfigurableApplicationContext context = null;
        String failingBootstrapEmail = generateUniqueEmail();

        try {
            // Start a real application context with WebApplicationType.NONE
            SpringApplicationBuilder builder = new SpringApplicationBuilder(EcommerceApplication.class)
                    .web(WebApplicationType.NONE);

            context = builder.run(
                    "--spring.datasource.hikari.maximum-pool-size=1",
                    "--app.admin.bootstrap.enabled=true",
                    "--app.admin.bootstrap.email=" + failingBootstrapEmail,
                    "--app.admin.bootstrap.password="
            );
        } catch (Exception ex) {
            startupException = ex;
        } finally {
            if (context != null) {
                context.close();
            }
        }

        try {
            assertNotNull(startupException, "Expected startup to fail with an exception, but it succeeded.");

            // Inspect the cause chain to find the IllegalStateException
            Throwable rootCause = startupException;
            while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
                rootCause = rootCause.getCause();
            }

            assertTrue(rootCause instanceof IllegalStateException, "Expected IllegalStateException, but got: " + rootCause.getClass().getName());
            assertTrue(rootCause.getMessage().contains("missing valid ADMIN_EMAIL or ADMIN_PASSWORD"), 
                "Expected failure message, but got: " + rootCause.getMessage());

            // Prove no database mutation occurred and sentinel is completely unchanged
            transactionTemplate.executeWithoutResult(status -> {
                assertEquals(initialCount, userRepository.count());
                
                User reloaded = userRepository.findById(sentinelId).orElseThrow();
                assertEquals(sentinelId, reloaded.getId());
                assertEquals(uniqueEmail, reloaded.getEmail());
                assertEquals("Sentinel", reloaded.getFirstName());
                assertEquals("User", reloaded.getLastName());
                assertEquals(sentinelHash, reloaded.getPasswordHash());
                assertEquals(RoleName.ROLE_CUSTOMER, reloaded.getRole().getName());
                assertTrue(reloaded.getActive());
            });
            
            // Prove no user exists with the failing unique email configured for bootstrap
            assertTrue(userRepository.findByEmail(failingBootstrapEmail).isEmpty());
        } finally {
            // Clean up persistent data for just the sentinel
            userRepository.deleteById(sentinelId);
        }
    }
}
