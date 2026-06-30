package com.clothingbrand.ecommerce.domain.catalog;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
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

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class AdminCategoryIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Setup Admin User
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        User admin = new User();
        admin.setEmail("admin-" + UUID.randomUUID() + "@test.com");
        admin.setFirstName("Test");
        admin.setLastName("Admin");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setRole(adminRole);
        admin.setActive(true);
        userRepository.save(admin);
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        // Setup Customer User
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        User customer = new User();
        customer.setEmail("customer-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setRole(customerRole);
        customer.setActive(true);
        userRepository.save(customer);
        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));
    }

    @Test
    void testCreateCategory_NoToken_Returns401() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto("Test", "Desc", null);
        mockMvc.perform(post("/api/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCreateCategory_InvalidToken_Returns401() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto("Test", "Desc", null);
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCreateCategory_CustomerToken_Returns403() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto("Test", "Desc", null);
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testCreateCategory_AdminToken_Returns201() throws Exception {
        String catName = "Cat-" + UUID.randomUUID();
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(catName, "Desc", "url.jpg");
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(catName))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.imageUrl").value("url.jpg"))
                .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void testCreateCategory_BlankName_Returns400() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto("   ", "Desc", null);
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Category name is required")));
    }

    @Test
    void testCreateCategory_DuplicateName_Returns409() throws Exception {
        String catName = "Cat-" + UUID.randomUUID();
        Category cat = new Category();
        cat.setName(catName);
        categoryRepository.save(cat);

        AdminCategoryRequestDto request = new AdminCategoryRequestDto(catName, "Desc", null);
        mockMvc.perform(post("/api/admin/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Category name already exists"));
    }

    @Test
    void testUpdateCategory_Success_Returns200() throws Exception {
        String originalName = "Cat-" + UUID.randomUUID();
        Category cat = new Category();
        cat.setName(originalName);
        cat = categoryRepository.save(cat);

        String newName = "Cat-" + UUID.randomUUID();
        AdminCategoryRequestDto request = new AdminCategoryRequestDto(newName, "New Desc", "new.jpg");
        mockMvc.perform(put("/api/admin/categories/" + cat.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.description").value("New Desc"));
    }

    @Test
    void testUpdateCategory_NotFound_Returns404() throws Exception {
        AdminCategoryRequestDto request = new AdminCategoryRequestDto("Name", "Desc", null);
        mockMvc.perform(put("/api/admin/categories/9999999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testDeleteCategory_Unreferenced_Returns204() throws Exception {
        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.save(cat);

        mockMvc.perform(delete("/api/admin/categories/" + cat.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCategory_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/9999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testDeleteCategory_WithProducts_Returns409() throws Exception {
        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.save(cat);

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Test Prod");
        prod.setActive(true);
        productRepository.save(prod);

        mockMvc.perform(delete("/api/admin/categories/" + cat.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete category as it is still referenced by one or more products"));
    }
}
