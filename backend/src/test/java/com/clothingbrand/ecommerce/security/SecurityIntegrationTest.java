package com.clothingbrand.ecommerce.security;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    private User activeCustomer;
    private User activeAdmin;
    private User inactiveCustomer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        activeCustomer = new User();
        activeCustomer.setEmail("customer@example.com");
        activeCustomer.setPasswordHash("hash");
        activeCustomer.setFirstName("Customer");
        activeCustomer.setLastName("User");
        activeCustomer.setRole(customerRole);
        activeCustomer.setActive(true);
        activeCustomer = userRepository.saveAndFlush(activeCustomer);

        activeAdmin = new User();
        activeAdmin.setEmail("admin@example.com");
        activeAdmin.setPasswordHash("hash");
        activeAdmin.setFirstName("Admin");
        activeAdmin.setLastName("User");
        activeAdmin.setRole(adminRole);
        activeAdmin.setActive(true);
        activeAdmin = userRepository.saveAndFlush(activeAdmin);

        inactiveCustomer = new User();
        inactiveCustomer.setEmail("inactive@example.com");
        inactiveCustomer.setPasswordHash("hash");
        inactiveCustomer.setFirstName("Inactive");
        inactiveCustomer.setLastName("User");
        inactiveCustomer.setRole(customerRole);
        inactiveCustomer.setActive(false);
        inactiveCustomer = userRepository.saveAndFlush(inactiveCustomer);
    }

    @Test
    void testPublicEndpoint_returns200WithoutToken() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_returns401WithoutToken() throws Exception {
        // Any unmapped endpoint defaults to authenticated()
        mockMvc.perform(get("/api/protected-dummy"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testAdminEndpoint_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testAdminEndpoint_returns403WithCustomerToken() throws Exception {
        String token = jwtService.generateToken(new UserDetailsImpl(activeCustomer));

        mockMvc.perform(get("/api/admin/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testAdminEndpoint_returns404WithAdminToken() throws Exception {
        // Since no controller exists for /api/admin/probe, 404 is the expected success for passing security
        String token = jwtService.generateToken(new UserDetailsImpl(activeAdmin));

        mockMvc.perform(get("/api/admin/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // 404 means it passed 401 and 403
    }

    @Test
    void testMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/protected-dummy")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testInactiveUser_returns401() throws Exception {
        // Generate valid token for user that is now inactive
        String token = jwtService.generateToken(new UserDetailsImpl(inactiveCustomer));

        mockMvc.perform(get("/api/protected-dummy")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testExpiredToken_returns401() throws Exception {
        java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(activeCustomer.getEmail())
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 100000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 50000)) // Expired in the past
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/protected-dummy")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testMalformedTokenOnPublicRoute_returns401() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
