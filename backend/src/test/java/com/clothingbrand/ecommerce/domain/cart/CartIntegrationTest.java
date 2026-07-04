package com.clothingbrand.ecommerce.domain.cart;

import com.clothingbrand.ecommerce.domain.catalog.Category;
import com.clothingbrand.ecommerce.domain.catalog.CategoryRepository;
import com.clothingbrand.ecommerce.domain.catalog.Product;
import com.clothingbrand.ecommerce.domain.catalog.ProductRepository;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariant;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class CartIntegrationTest {

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
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private User admin;
    private User customer;
    private User customer2;

    private String adminToken;
    private String customerToken;
    private String customer2Token;

    private Product testProduct;
    private Product testProduct2;
    private Product inactiveProduct;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        admin = new User();
        admin.setEmail("admin-" + UUID.randomUUID() + "@test.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setRole(adminRole);
        admin.setActive(true);
        userRepository.save(admin);
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        customer = new User();
        customer.setEmail("customer1-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Customer");
        customer.setLastName("One");
        customer.setPasswordHash(passwordEncoder.encode("password"));
        customer.setRole(customerRole);
        customer.setActive(true);
        userRepository.save(customer);
        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));

        customer2 = new User();
        customer2.setEmail("customer2-" + UUID.randomUUID() + "@test.com");
        customer2.setFirstName("Customer");
        customer2.setLastName("Two");
        customer2.setPasswordHash(passwordEncoder.encode("password"));
        customer2.setRole(customerRole);
        customer2.setActive(true);
        userRepository.save(customer2);
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));

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

        Product inactiveProd = new Product();
        inactiveProd.setCategory(cat);
        inactiveProd.setName("Inactive Product");
        inactiveProd.setActive(false);
        inactiveProduct = productRepository.save(inactiveProd);
    }

    private ProductVariant createSavedVariant(Product product, String sku, String size, String color, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        product.getVariants().add(variant);
        productRepository.save(product);
        return variant;
    }

    // --- Security and Role Boundaries ---

    @Test
    void test1_MissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/cart/items/1").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/cart/items/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void test2_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void test3_ValidCustomerToken_Returns200() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void test4_ValidAdminToken_Returns403() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // --- Empty Cart and Lazy Creation ---

    @Test
    void test5_CustomerNoCart_ReturnsEmptyResponse_NoCartCreated() throws Exception {
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.cartTotal").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(0));

        assertFalse(cartRepository.findByUserId(customer.getId()).isPresent());
    }

    @Test
    void test6_InvalidFirstAdd_Fails_NoCartCreated() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\": 999999, \"quantity\": 1}"))
                .andExpect(status().isNotFound());

        assertFalse(cartRepository.findByUserId(customer.getId()).isPresent());
    }

    // --- Add Item Behavior ---

    @Test
    void test7_AddValidVariant_Success() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADD1", "S", "Red", 10, "15.50");
        CartItemRequestDto request = new CartItemRequestDto(variant.getId(), 2);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].variantId").value(variant.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(15.50))
                .andExpect(jsonPath("$.items[0].lineTotal").value(31.00))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.cartTotal").value(31.00))
                .andExpect(jsonPath("$.totalQuantity").value(2));

        Cart savedCart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(1, savedCart.getItems().size());
    }

    @Test
    void test8_AddSameVariantAgain_Increments() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-ADD2", "M", "Blue", 10, "10.00");
        CartItemRequestDto request1 = new CartItemRequestDto(variant.getId(), 2);
        CartItemRequestDto request2 = new CartItemRequestDto(variant.getId(), 3);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.cartTotal").value(50.00))
                .andExpect(jsonPath("$.totalQuantity").value(5));

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void test9_AddDifferentVariant() throws Exception {
        ProductVariant var1 = createSavedVariant(testProduct, "SKU-ADD3-1", "S", "Red", 10, "10.00");
        ProductVariant var2 = createSavedVariant(testProduct, "SKU-ADD3-2", "M", "Blue", 10, "20.00");

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(var1.getId(), 1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(var2.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.cartTotal").value(50.00))
                .andExpect(jsonPath("$.totalQuantity").value(3));
    }

    @Test
    void test10_AddMissingVariant_Returns404() throws Exception {
        CartItemRequestDto request = new CartItemRequestDto(999999L, 1);
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void test11_AddVariantFromInactiveProduct_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(inactiveProduct, "SKU-INACT1", "S", "Red", 10, "10.00");
        CartItemRequestDto request = new CartItemRequestDto(variant.getId(), 1);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertFalse(cartRepository.findByUserId(customer.getId()).isPresent());
    }

    @Test
    void test12_AddQuantityExceedingStock_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-EXC1", "S", "Red", 5, "10.00");
        CartItemRequestDto request = new CartItemRequestDto(variant.getId(), 6);

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertFalse(cartRepository.findByUserId(customer.getId()).isPresent());
    }

    @Test
    void test13_AddInvalidInput_Returns400() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": 1}")) // missing variant
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\": 1, \"quantity\": 0}")) // zero quantity
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"variantId\": 1, \"quantity\": -1}")) // negative quantity
                .andExpect(status().isBadRequest());
    }

    @Test
    void test14_DuplicateAddExceedingStock_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DUPEXC", "S", "Red", 5, "10.00");

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 3))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 3))))
                .andExpect(status().isConflict());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(3, cart.getItems().iterator().next().getQuantity());
    }

    // --- Update Quantity Behavior ---

    @Test
    void test15_ReplaceQuantitySuccessfully() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-UPD1", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequestDto(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.cartTotal").value(50.00))
                .andExpect(jsonPath("$.totalQuantity").value(5));
    }

    @Test
    void test16_InvalidUpdateInput_Returns400() throws Exception {
        mockMvc.perform(put("/api/cart/items/1")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test17_UpdateQuantityAboveStock_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-UPD2", "S", "Red", 5, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequestDto(6))))
                .andExpect(status().isConflict());
    }

    @Test
    void test18_UpdateItemUnderInactiveProduct_Returns409() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-UPD3", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        testProduct.setActive(false);
        productRepository.save(testProduct);

        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequestDto(5))))
                .andExpect(status().isConflict());
    }

    @Test
    void test19_UpdateMissingCartItem_Returns404() throws Exception {
        mockMvc.perform(put("/api/cart/items/999999")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequestDto(5))))
                .andExpect(status().isNotFound());
    }

    @Test
    void test20_UpdateAnotherCustomersCartItem_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-UPD4", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))))
                .andExpect(status().isOk());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        // Customer 2 tries to update Customer 1's item
        mockMvc.perform(put("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customer2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemUpdateRequestDto(5))))
                .andExpect(status().isNotFound());
    }

    // --- Remove Behavior ---

    @Test
    void test21_RemoveOneCartItem() throws Exception {
        ProductVariant var1 = createSavedVariant(testProduct, "SKU-DEL1", "S", "Red", 10, "10.00");
        ProductVariant var2 = createSavedVariant(testProduct, "SKU-DEL2", "M", "Blue", 10, "20.00");

        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(var1.getId(), 1))));
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(var2.getId(), 2))));

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long item1Id = cart.getItems().stream().filter(i -> i.getProductVariant().getId().equals(var1.getId())).findFirst().orElseThrow().getId();

        mockMvc.perform(delete("/api/cart/items/" + item1Id)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.cartTotal").value(40.00));
    }

    @Test
    void test22_RemoveFinalCartItem_CartRemains() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DEL3", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 1))));

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        mockMvc.perform(delete("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());

        assertTrue(cartRepository.findByUserId(customer.getId()).isPresent());

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void test23_RemoveMissingCartItem_Returns404() throws Exception {
        mockMvc.perform(delete("/api/cart/items/999999")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void test24_RemoveAnotherCustomersCartItem_Returns404() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DEL4", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 1))));

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        Long cartItemId = cart.getItems().iterator().next().getId();

        mockMvc.perform(delete("/api/cart/items/" + cartItemId)
                .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isNotFound());
    }

    // --- Dynamic Price and Availability ---

    @Test
    void test25_DynamicCurrentPrice() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DYN1", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))));

        variant.setPrice(new BigDecimal("15.00"));
        productVariantRepository.save(variant);

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(15.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(30.00))
                .andExpect(jsonPath("$.cartTotal").value(30.00));
    }

    @Test
    void test26_AvailabilityAfterStockDecreases() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DYN2", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 5))));

        variant.setStockQuantity(2); // Lower than cart quantity 5
        productVariantRepository.save(variant);

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].stockQuantity").doesNotExist());
    }

    @Test
    void test27_AvailabilityAfterProductDeactivation() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-DYN3", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))));

        testProduct.setActive(false);
        productRepository.save(testProduct);

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].available").value(false));
    }

    // --- Cart Data Privacy ---

    @Test
    void test28_CartApiDataHiding() throws Exception {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-PRIV1", "S", "Red", 10, "10.00");
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemRequestDto(variant.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.items[0].cartId").doesNotExist())
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.cartId").doesNotExist());
    }
}
