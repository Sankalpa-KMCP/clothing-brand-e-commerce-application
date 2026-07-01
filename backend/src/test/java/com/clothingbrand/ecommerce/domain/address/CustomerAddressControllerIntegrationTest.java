package com.clothingbrand.ecommerce.domain.address;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CustomerAddressControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private User customer;
    private User customer2;
    private User admin;
    private String customerToken;
    private String customer2Token;
    private String adminToken;

    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdAddressIds.clear();
        createdUserIds.clear();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        customer = createUser(customerRole, "address-http-a");
        customer2 = createUser(customerRole, "address-http-b");
        admin = createUser(adminRole, "address-http-admin");

        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (CustomerAddress address : customerAddressRepository.findAll()) {
                if (createdUserIds.contains(address.getUser().getId()) && !createdAddressIds.contains(address.getId())) {
                    createdAddressIds.add(address.getId());
                }
            }

            for (Long addressId : createdAddressIds) {
                if (customerAddressRepository.existsById(addressId)) {
                    customerAddressRepository.deleteById(addressId);
                }
            }

            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
        });

        transactionTemplate.executeWithoutResult(status -> {
            for (Long addressId : createdAddressIds) {
                assertTrue(customerAddressRepository.findById(addressId).isEmpty(), "Address leak: " + addressId);
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "User leak: " + userId);
            }
        });
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("Http");
        user.setLastName(label);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.saveAndFlush(user);
        createdUserIds.add(user.getId());
        return user;
    }

    private CustomerAddressRequestDto createValidRequest() {
        return new CustomerAddressRequestDto(
                "Home",
                "John Doe",
                "1234567890",
                "123 Main St",
                null,
                "Metropolis",
                null,
                null,
                "USA"
        );
    }

    @Test
    void customerCreatesAddress_returns201AndSafeJson_startsNonDefault() throws Exception {
        CustomerAddressRequestDto req = createValidRequest();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.label").value("Home"))
                .andExpect(jsonPath("$.recipientName").value("John Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("1234567890"))
                .andExpect(jsonPath("$.addressLine1").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("Metropolis"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void customerListsOwnAddresses_returns200AndStableOrder() throws Exception {
        CustomerAddressRequestDto req = createValidRequest();

        MvcResult res1 = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        MvcResult res2 = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // foreign address
        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // set id1 as default
        mockMvc.perform(patch("/api/addresses/" + id1 + "/default")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id1))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].id").value(id2))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void customerReadsOwnAddress_returns200() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/addresses/" + id)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.recipientName").value("John Doe"));
    }

    @Test
    void customerUpdatesOwnAddress_returns200AndPreservesDefaultState() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/addresses/" + id + "/default")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        CustomerAddressRequestDto updateReq = new CustomerAddressRequestDto(
                "Work", "Jane Doe", "0987654321", "456 Side St", null, "Gotham", null, null, "USA"
        );

        mockMvc.perform(put("/api/addresses/" + id)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Work"))
                .andExpect(jsonPath("$.recipientName").value("Jane Doe"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void customerDeletesOwnAddress_returns204() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/addresses/" + id)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/addresses/" + id)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerSetsOwnedAddressDefault_returns200() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/addresses/" + id + "/default")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    void emptyAddressBookReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void foreignAddressOperations_returnNotFound() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        Long foreignId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/addresses/" + foreignId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(put("/api/addresses/" + foreignId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidRequest())))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/addresses/" + foreignId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/addresses/" + foreignId + "/default")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFields_returns400() throws Exception {
        CustomerAddressRequestDto invalid = new CustomerAddressRequestDto(
                "Home", "", "", "", null, "", null, null, ""
        );
        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void adminToken_returns403() throws Exception {
        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
