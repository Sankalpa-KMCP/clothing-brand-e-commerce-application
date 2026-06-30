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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminProductVariantIntegrationTest {

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
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    private String adminToken;
    private String customerToken;
    private Product testProduct;
    private Product testProduct2;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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

        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.save(cat);

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Test Product");
        prod.setActive(true);
        testProduct = productRepository.save(prod);

        Product prod2 = new Product();
        prod2.setCategory(cat);
        prod2.setName("Test Product 2");
        prod2.setActive(true);
        testProduct2 = productRepository.save(prod2);
    }

    private ProductVariant createSavedVariant(Product product, String sku, String size, String color) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(new BigDecimal("10.00"));
        variant.setStockQuantity(5);
        variant = productVariantRepository.save(variant);
        product.getVariants().add(variant);
        productRepository.save(product);
        return variant;
    }

    @Test
    void testVariantEndpoints_Security_NoToken_Returns401() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU", "S", "Red", new BigDecimal("10.0"));

        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/admin/products/" + testProduct.getId() + "/variants/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateVariant_InvalidToken_Returns401() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU", "S", "Red", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateVariant_CustomerToken_Returns403() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU", "S", "Red", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateVariant_AdminToken_Returns201() throws Exception {
        String sku = "SKU-" + UUID.randomUUID();
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto(sku, "M", "Blue", new BigDecimal("25.50"));

        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.size").value("M"))
                .andExpect(jsonPath("$.color").value("Blue"))
                .andExpect(jsonPath("$.price").value(25.50))
                .andExpect(jsonPath("$.stockQuantity").value(0)); // Default stock
    }

    @Test
    void testCreateVariant_MissingProduct_Returns404() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-" + UUID.randomUUID(), "S", "Red", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/999999/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testCreateVariant_BlankRequiredFields_Returns400() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("   ", "", null, null);
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testCreateVariant_NonPositivePrice_Returns400() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-PRICE", "S", "Red", new BigDecimal("-5.00"));
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        AdminProductVariantRequestDto request2 = new AdminProductVariantRequestDto("SKU-PRICE2", "S", "Red", BigDecimal.ZERO);
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateVariant_DuplicateSku_Returns409() throws Exception {
        String sku = "SKU-DUP-" + UUID.randomUUID();
        createSavedVariant(testProduct, sku, "S", "Red");

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto(sku, "M", "Blue", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testCreateVariant_DuplicateSizeColorForProduct_Returns409() throws Exception {
        createSavedVariant(testProduct, "SKU-S-RED", "S", "Red");

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-DIFF", "S", "Red", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/" + testProduct.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testCreateVariant_SameSizeColorDifferentProduct_Succeeds() throws Exception {
        createSavedVariant(testProduct, "SKU-1", "S", "Red");

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-2", "S", "Red", new BigDecimal("10.0"));
        mockMvc.perform(post("/api/admin/products/" + testProduct2.getId() + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void testUpdateVariant_ValidAdmin_Returns200AndPreservesStock() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-UPD", "S", "Red");
        // Initial stock is 5
        assertEquals(5, variant.getStockQuantity());

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-UPD-NEW", "M", "Blue", new BigDecimal("15.00"));

        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-UPD-NEW"))
                .andExpect(jsonPath("$.size").value("M"))
                .andExpect(jsonPath("$.color").value("Blue"))
                .andExpect(jsonPath("$.price").value(15.00))
                .andExpect(jsonPath("$.stockQuantity").value(5)); // Preserved

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals("SKU-UPD-NEW", updated.getSku());
        assertEquals("M", updated.getSize());
        assertEquals(5, updated.getStockQuantity());
    }

    @Test
    void testUpdateVariant_MissingProduct_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-MISSP", "S", "Red");
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-NEW", "M", "Blue", new BigDecimal("15.00"));

        mockMvc.perform(put("/api/admin/products/999999/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testUpdateVariant_MissingVariant_Returns404() throws Exception {
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-NEW", "M", "Blue", new BigDecimal("15.00"));

        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/999999")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testUpdateVariant_WrongProductOwnership_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct2, "SKU-WRONG", "S", "Red");
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-NEW", "M", "Blue", new BigDecimal("15.00"));

        // Use testProduct.getId() instead of testProduct2
        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testUpdateVariant_DuplicateSku_Returns409() throws Exception {
        createSavedVariant(testProduct, "SKU-EXIST", "S", "Red");
        ProductVariant variant2 = createSavedVariant(testProduct, "SKU-EXIST2", "M", "Blue");

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-EXIST", "L", "Green", new BigDecimal("15.00"));

        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/" + variant2.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testUpdateVariant_DuplicateSizeColorForProduct_Returns409() throws Exception {
        createSavedVariant(testProduct, "SKU-EX-" + UUID.randomUUID(), "S", "Red");
        ProductVariant variant2 = createSavedVariant(testProduct, "SKU-EX-" + UUID.randomUUID(), "M", "Blue");

        String uniqueSku = "SKU-UNIQ-" + UUID.randomUUID();
        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto(uniqueSku, "S", "Red", new BigDecimal("15.00"));

        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/" + variant2.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        ProductVariant unchanged = productVariantRepository.findById(variant2.getId()).orElseThrow();
        assertEquals("M", unchanged.getSize());
    }

    @Test
    void testDeleteVariant_ValidAdmin_Returns204() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DEL", "S", "Red");

        mockMvc.perform(delete("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertFalse(productVariantRepository.existsById(variant.getId()));
        assertTrue(productRepository.existsById(testProduct.getId())); // Parent product safely usable
    }

    @Test
    void testDeleteVariant_MissingVariant_Returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/products/" + testProduct.getId() + "/variants/999999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteVariant_WrongProductOwnership_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct2, "SKU-DEL-WRONG", "S", "Red");

        mockMvc.perform(delete("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteVariant_MissingParentProduct_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DEL-MISSING-" + UUID.randomUUID(), "S", "Red");

        mockMvc.perform(delete("/api/admin/products/999999/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertTrue(productVariantRepository.existsById(variant.getId()));
        assertTrue(productRepository.existsById(testProduct.getId()));
    }

    @Test
    void testPublicDataHidingAndZeroVariantBehavior() throws Exception {
        // Zero variant check
        mockMvc.perform(get("/api/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants").isEmpty());

        ProductVariant variant = createSavedVariant(testProduct, "SKU-HIDE", "S", "Red");

        AdminProductVariantRequestDto request = new AdminProductVariantRequestDto("SKU-HIDE-UPD", "M", "Blue", new BigDecimal("25.00"));
        mockMvc.perform(put("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify public API still hides SKU and stock
        mockMvc.perform(get("/api/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.variants[0].size").value("M"));
    }

    @Test
    void testStockAdjustment_Security_NoToken_Returns401() throws Exception {
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);
        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testStockAdjustment_InvalidToken_Returns401() throws Exception {
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);
        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/1/stock")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testStockAdjustment_CustomerToken_Returns403() throws Exception {
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);
        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/1/stock")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStockAdjustment_PositiveAdjustment_Returns200() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-POS-" + UUID.randomUUID(), "S", "Red");
        assertEquals(5, variant.getStockQuantity());

        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(10);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15))
                .andExpect(jsonPath("$.sku").value(variant.getSku()));

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(15, updated.getStockQuantity());
        assertEquals(variant.getSku(), updated.getSku());
    }

    @Test
    void testStockAdjustment_NegativeAdjustment_NonNegative_Returns200() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-NEG-" + UUID.randomUUID(), "M", "Blue");
        assertEquals(5, variant.getStockQuantity());

        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(-3);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(2));

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(2, updated.getStockQuantity());
        assertEquals(variant.getSku(), updated.getSku());
    }

    @Test
    void testStockAdjustment_ZeroAdjustment_Returns400() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-ZERO-" + UUID.randomUUID(), "L", "Green");

        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(0);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        ProductVariant unchanged = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
    }

    @Test
    void testStockAdjustment_MissingOrNullAdjustment_Returns400() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-NULL-" + UUID.randomUUID(), "XL", "Black");

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        ProductVariant unchanged = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
    }

    @Test
    void testStockAdjustment_MissingProduct_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-MISSP-" + UUID.randomUUID(), "S", "Red");
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);

        mockMvc.perform(patch("/api/admin/products/999999/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        ProductVariant unchanged = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
    }

    @Test
    void testStockAdjustment_MissingVariant_Returns404() throws Exception {
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/999999/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testStockAdjustment_WrongParentPath_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct2, "SKU-ADJ-WRONG-" + UUID.randomUUID(), "S", "Red");
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(5);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        ProductVariant unchanged = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
    }

    @Test
    void testStockAdjustment_NegativeAdjustment_InsufficientStock_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-INSUFF-" + UUID.randomUUID(), "M", "Blue");
        assertEquals(5, variant.getStockQuantity());

        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(-10);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        ProductVariant unchanged = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
        assertEquals(variant.getSku(), unchanged.getSku());
    }

    @Test
    void testStockAdjustment_PublicCatalogRegression_HidesStockAndSku() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADJ-PUB-HIDE-" + UUID.randomUUID(), "S", "Red");
        AdminStockAdjustmentRequestDto request = new AdminStockAdjustmentRequestDto(15);

        mockMvc.perform(patch("/api/admin/products/" + testProduct.getId() + "/variants/" + variant.getId() + "/stock")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].sku").doesNotExist())
                .andExpect(jsonPath("$.variants[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.variants[0].size").exists());
    }
}
