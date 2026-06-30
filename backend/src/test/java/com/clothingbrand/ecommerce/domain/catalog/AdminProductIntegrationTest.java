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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminProductIntegrationTest {

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
    private Category testCategory;

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

        // Setup a category
        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        testCategory = categoryRepository.save(cat);
    }

    @Test
    void testCreateProduct_NoToken_Returns401() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Test Product", "Desc", "url", true);
        mockMvc.perform(post("/api/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCreateProduct_InvalidToken_Returns401() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Test Product", "Desc", "url", true);
        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCreateProduct_CustomerToken_Returns403() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Test Product", "Desc", "url", true);
        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testCreateProduct_AdminToken_Returns201() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Test Product", "Desc", "url", true);
        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.categoryId").value(testCategory.getId()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testCreateProduct_MissingCategory_Returns404() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(999999L, "Test Product", "Desc", "url", true);
        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testCreateProduct_BlankRequiredFields_Returns400() throws Exception {
        // Missing name and category ID
        AdminProductRequestDto request = new AdminProductRequestDto(null, "   ", "Desc", "url", null);
        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testUpdateProduct_Success_Returns200() throws Exception {
        // Create initial product
        Product prod = new Product();
        prod.setCategory(testCategory);
        prod.setName("Initial Name");
        prod.setActive(true);
        prod = productRepository.save(prod);

        // Update it
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Updated Name", "New Desc", "new.url", false);
        mockMvc.perform(put("/api/admin/products/" + prod.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.active").value(false));

        // Verify public API exclusion (since active = false)
        mockMvc.perform(get("/api/products/" + prod.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProduct_NonexistentProduct_Returns404() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Name", "Desc", "url", true);
        mockMvc.perform(put("/api/admin/products/999999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testUpdateProduct_MissingTargetCategory_Returns404() throws Exception {
        Product prod = new Product();
        prod.setCategory(testCategory);
        prod.setName("Initial Name");
        prod.setActive(true);
        prod = productRepository.save(prod);

        AdminProductRequestDto request = new AdminProductRequestDto(999999L, "Updated Name", "Desc", "url", true);
        mockMvc.perform(put("/api/admin/products/" + prod.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testUpdateProduct_NoToken_Returns401() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Test Product", "Desc", "url", true);
        mockMvc.perform(put("/api/admin/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testCreateProduct_NullableFields_Returns201AndPersists() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Nullable Product", null, null, true);

        mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nullable Product"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        Product savedProduct = productRepository.findAll().stream()
                .filter(p -> p.getName().equals("Nullable Product"))
                .findFirst().orElseThrow();

        assertNull(savedProduct.getDescription());
        assertNull(savedProduct.getImageUrl());
    }

    @Test
    void testCreateProduct_FullAssertions() throws Exception {
        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Full Product", "Full Desc", "full.url", true);

        String responseContent = mockMvc.perform(post("/api/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Full Product"))
                .andExpect(jsonPath("$.categoryId").value(testCategory.getId()))
                .andExpect(jsonPath("$.description").value("Full Desc"))
                .andExpect(jsonPath("$.imageUrl").value("full.url"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(responseContent).get("id").asLong();

        Product savedProduct = productRepository.findById(newId).orElseThrow();
        assertEquals("Full Product", savedProduct.getName());
        assertEquals(testCategory.getId(), savedProduct.getCategory().getId());
        assertEquals("Full Desc", savedProduct.getDescription());
        assertEquals("full.url", savedProduct.getImageUrl());
        assertTrue(savedProduct.getActive());
    }

    @Test
    void testUpdateProduct_CategoryReassignmentAndFullAssertions() throws Exception {
        Category cat2 = new Category();
        cat2.setName("Cat2-" + UUID.randomUUID());
        cat2 = categoryRepository.save(cat2);

        Product prod = new Product();
        prod.setCategory(testCategory);
        prod.setName("Old Name");
        prod.setDescription("Old Desc");
        prod.setImageUrl("old.url");
        prod.setActive(true);
        prod = productRepository.save(prod);

        AdminProductRequestDto request = new AdminProductRequestDto(cat2.getId(), "New Name", "New Desc", "new.url", false);

        mockMvc.perform(put("/api/admin/products/" + prod.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.categoryId").value(cat2.getId()))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.imageUrl").value("new.url"))
                .andExpect(jsonPath("$.active").value(false));

        Product updatedProd = productRepository.findById(prod.getId()).orElseThrow();
        assertEquals(cat2.getId(), updatedProd.getCategory().getId());
        assertEquals("New Name", updatedProd.getName());
        assertEquals("New Desc", updatedProd.getDescription());
        assertEquals("new.url", updatedProd.getImageUrl());
        assertFalse(updatedProd.getActive());

        assertTrue(categoryRepository.existsById(testCategory.getId()));
    }

    @Test
    void testPublicZeroVariantBehavior() throws Exception {
        Product prod = new Product();
        prod.setCategory(testCategory);
        prod.setName("Zero Variant Product");
        prod.setActive(true);
        prod = productRepository.save(prod);

        mockMvc.perform(get("/api/products/" + prod.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Zero Variant Product"))
                .andExpect(jsonPath("$.variants").isArray())
                .andExpect(jsonPath("$.variants").isEmpty());

        mockMvc.perform(get("/api/products").param("query", "Zero Variant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Zero Variant Product"))
                .andExpect(jsonPath("$.content[0].startingPrice").doesNotExist());
    }

    @Test
    void testPublicHidingAfterAdminUpdate() throws Exception {
        Product prod = new Product();
        prod.setCategory(testCategory);
        prod.setName("Product With Variant");
        prod.setActive(true);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(prod);
        variant.setSku("SKU-" + UUID.randomUUID());
        variant.setSize("L");
        variant.setColor("Blue");
        variant.setPrice(new java.math.BigDecimal("25.00"));
        variant.setStockQuantity(10);
        prod.getVariants().add(variant);

        prod = productRepository.save(prod);

        AdminProductRequestDto request = new AdminProductRequestDto(testCategory.getId(), "Updated Name", "Desc", "url", true);
        mockMvc.perform(put("/api/admin/products/" + prod.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + prod.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.variants[0].price").value(25.0));
    }
}
