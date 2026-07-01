package com.clothingbrand.ecommerce.domain.user;

import com.clothingbrand.ecommerce.EcommerceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EcommerceApplication.class)
@ActiveProfiles("test")
public class RefreshIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User testUser;
    private final List<UUID> trackedFamilies = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ROLE_CUSTOMER);
                    return roleRepository.save(role);
                });

        testUser = new User();
        testUser.setEmail("refresh_test_user_" + UUID.randomUUID() + "@example.com");
        testUser.setFirstName("Refresh");
        testUser.setLastName("User");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setRole(customerRole);
        testUser.setActive(true);
        testUser = userRepository.saveAndFlush(testUser);
    }

    @AfterEach
    void tearDown() {
        trackedFamilies.clear();

        if (testUser != null && testUser.getId() != null) {
            userRepository.deleteById(testUser.getId());
        }
    }

    @Test
    void registerAndLogin_returnsRefreshToken() throws Exception {
        // Test Register
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setEmail("new_user_" + UUID.randomUUID() + "@example.com");
        registerRequest.setPassword("securePassword123!");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        AuthResponseDto registerResponse = objectMapper.readValue(registerResult.getResponse().getContentAsString(), AuthResponseDto.class);
        assertNotNull(registerResponse.getRefreshToken());

        // Test Login
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail(registerRequest.getEmail());
        loginRequest.setPassword("securePassword123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        AuthResponseDto loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponseDto.class);
        assertNotNull(loginResponse.getRefreshToken());

        // Cleanup dynamically created user
        userRepository.deleteById(registerResponse.getUser().getId());
    }

    @Test
    void refresh_withValidToken_returnsNewTokenPair() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponseDto.class);
        String initialRefreshToken = loginResponse.getRefreshToken();
        assertNotNull(initialRefreshToken);

        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken(initialRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void logout_withValidToken_returnsNoContent_andSubsequentRefreshFails() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponseDto.class);
        String refreshToken = loginResponse.getRefreshToken();

        LogoutRequestDto logoutRequest = new LogoutRequestDto();
        logoutRequest.setRefreshToken(refreshToken);

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Subsequent refresh should fail
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refresh_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withMissingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withInvalidBearerToken_preservesFilterRejection() throws Exception {
        RefreshTokenRequestDto refreshRequest = new RefreshTokenRequestDto();
        refreshRequest.setRefreshToken("dummy-token");

        mockMvc.perform(post("/api/auth/refresh")
                .header("Authorization", "Bearer invalid-jwt-token-string")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }
}
