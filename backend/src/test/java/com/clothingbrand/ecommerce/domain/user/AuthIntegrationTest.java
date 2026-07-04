package com.clothingbrand.ecommerce.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.clothingbrand.ecommerce.domain.order.CustomerOrderRepository;
import com.clothingbrand.ecommerce.domain.order.OrderStatusHistoryRepository;
import com.clothingbrand.ecommerce.domain.order.OrderDeliveryAddressRepository;
import com.clothingbrand.ecommerce.domain.order.OrderItemRepository;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;
    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        orderStatusHistoryRepository.deleteAll();
        orderDeliveryAddressRepository.deleteAll();
        orderItemRepository.deleteAll();
        customerOrderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testValidRegistration() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane@example.com");
        request.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Jane"))
                .andExpect(jsonPath("$.user.lastName").value("Doe"))
                .andExpect(jsonPath("$.user.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.active").doesNotExist())
                .andExpect(jsonPath("$.user.createdAt").doesNotExist())
                .andExpect(jsonPath("$.user.updatedAt").doesNotExist());

        User savedUser = userRepository.findByEmail("jane@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("SafePassword123!", savedUser.getPasswordHash()));
        assertEquals(RoleName.ROLE_CUSTOMER, savedUser.getRole().getName());
    }

    @Test
    void testRegistrationNormalizesAndTrims() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("  Jane  ");
        request.setLastName("  Doe  ");
        request.setEmail("JANE@EXAMPLE.COM");
        request.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.firstName").value("Jane"))
                .andExpect(jsonPath("$.user.lastName").value("Doe"));
    }

    @Test
    void testDuplicateEmailReturns409() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane@example.com");
        request.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt duplicate with case variant
        RegisterRequestDto duplicate = new RegisterRequestDto();
        duplicate.setFirstName("Jane2");
        duplicate.setLastName("Doe2");
        duplicate.setEmail("JANE@example.com");
        duplicate.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void testInvalidRegisterPayloadReturns400() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFirstName("");
        request.setLastName("Doe");
        request.setEmail("invalid-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("firstName: First name is required")))
                .andExpect(jsonPath("$.message", containsString("email: Email should be valid")))
                .andExpect(jsonPath("$.message", containsString("password: Password must be at least 8 characters long")));
    }

    @Test
    void testValidLogin() throws Exception {
        // Register first
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("SafePassword123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.email").value("jane@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void testWrongPasswordReturns401() throws Exception {
        // Register first
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("SafePassword123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testUnknownEmailReturns401() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("unknown@example.com");
        loginRequest.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testInactiveUserLoginReturns401() throws Exception {
        // Register an active user first
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("inactive@example.com");
        registerRequest.setPassword("SafePassword123!");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Make the user inactive directly in the database
        User user = userRepository.findByEmail("inactive@example.com").orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        // Attempt login with correct credentials
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("inactive@example.com");
        loginRequest.setPassword("SafePassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void testGetMeWithValidToken() throws Exception {
        // Register first
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("SafePassword123!");
        
        String responseBody = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();

        // Get Me
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void testGetMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
