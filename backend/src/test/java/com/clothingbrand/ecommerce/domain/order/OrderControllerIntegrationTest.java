package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartItem;
import com.clothingbrand.ecommerce.domain.cart.CartItemRepository;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
import com.clothingbrand.ecommerce.domain.order.OrderDeliveryAddressRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OrderControllerIntegrationTest {

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
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

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
    private Category category;
    private Product activeProduct;
    private Product inactiveProduct;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdDeliveryAddressIds.clear();
        createdAddressIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        customer = createUser(customerRole, "customer-a");
        customer2 = createUser(customerRole, "customer-b");
        admin = createUser(adminRole, "admin");

        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        category = new Category();
        category.setName("Checkout-Cat-" + UUID.randomUUID());
        category = categoryRepository.save(category);
        createdCategoryIds.add(category.getId());

        activeProduct = createProduct(true);
        inactiveProduct = createProduct(false);
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (CustomerOrder order : customerOrderRepository.findAll()) {
                if (createdUserIds.contains(order.getUser().getId()) && !createdOrderIds.contains(order.getId())) {
                    createdOrderIds.add(order.getId());
                }
            }

                        for (Long deliveryId : createdDeliveryAddressIds) {
                if (orderDeliveryAddressRepository.existsById(deliveryId)) {
                    orderDeliveryAddressRepository.deleteById(deliveryId);
                }
            }
            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    orderDeliveryAddressRepository.findByOrderId(orderId)
                        .ifPresent(delivery -> orderDeliveryAddressRepository.delete(delivery));
                    order.getItems().clear();
                    customerOrderRepository.saveAndFlush(order);
                    customerOrderRepository.deleteById(orderId);
                    customerOrderRepository.flush();
                });
            }

            for (Long cartItemId : createdCartItemIds) {
                if (cartItemRepository.existsById(cartItemId)) {
                    cartItemRepository.deleteById(cartItemId);
                }
            }

            for (Long cartId : createdCartIds) {
                cartRepository.findById(cartId).ifPresent(cart -> {
                    cart.getItems().clear();
                    cartRepository.saveAndFlush(cart);
                    cartRepository.deleteById(cartId);
                    cartRepository.flush();
                });
            }

            for (Long variantId : createdVariantIds) {
                if (productVariantRepository.existsById(variantId)) {
                    productVariantRepository.deleteById(variantId);
                }
            }

            for (Long productId : createdProductIds) {
                if (productRepository.existsById(productId)) {
                    productRepository.deleteById(productId);
                }
            }

            for (Long categoryId : createdCategoryIds) {
                if (categoryRepository.existsById(categoryId)) {
                    categoryRepository.deleteById(categoryId);
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
            for (Long orderId : createdOrderIds) {
                            for (Long deliveryId : createdDeliveryAddressIds) {
                assertTrue(orderDeliveryAddressRepository.findById(deliveryId).isEmpty(), "Delivery leak: " + deliveryId);
            }
            for (Long addressId : createdAddressIds) {
                assertTrue(customerAddressRepository.findById(addressId).isEmpty(), "Address leak: " + addressId);
            }
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak: " + orderId);
            }
            for (Long cartItemId : createdCartItemIds) {
                assertTrue(cartItemRepository.findById(cartItemId).isEmpty(), "CartItem leak: " + cartItemId);
            }
            for (Long cartId : createdCartIds) {
                assertTrue(cartRepository.findById(cartId).isEmpty(), "Cart leak: " + cartId);
            }
            for (Long variantId : createdVariantIds) {
                assertTrue(productVariantRepository.findById(variantId).isEmpty(), "Variant leak: " + variantId);
            }
            for (Long productId : createdProductIds) {
                assertTrue(productRepository.findById(productId).isEmpty(), "Product leak: " + productId);
            }
            for (Long categoryId : createdCategoryIds) {
                assertTrue(categoryRepository.findById(categoryId).isEmpty(), "Category leak: " + categoryId);
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "User leak: " + userId);
            }
        });
    }

    @Test
    void successfulCustomerCheckout_returnsCreatedSafeOrderAndMutatesOnlyCustomerFixtures() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 8, "19.99", "M", "Black");
        Cart customerCart = addCartItem(customer, variant, 2);
        ProductVariant otherVariant = createVariant(activeProduct, 5, "11.00", "L", "Green");
        Cart otherCart = addCartItem(customer2, otherVariant, 1);

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.subtotal").value(39.98))
                .andExpect(jsonPath("$.total").value(39.98))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value(activeProduct.getName()))
                .andExpect(jsonPath("$.items[0].productImageUrl").value(activeProduct.getImageUrl()))
                .andExpect(jsonPath("$.items[0].size").value("M"))
                .andExpect(jsonPath("$.items[0].color").value("Black"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.99))
                .andExpect(jsonPath("$.items[0].lineTotal").value(39.98))
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.productId").doesNotExist())
                .andExpect(jsonPath("$.variantId").doesNotExist())
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.items[0].cartId").doesNotExist())
                .andExpect(jsonPath("$.items[0].productId").doesNotExist())
                .andExpect(jsonPath("$.items[0].variantId").doesNotExist())
                .andExpect(jsonPath("$.items[0].originalProductId").doesNotExist())
                .andExpect(jsonPath("$.items[0].originalVariantId").doesNotExist())
                .andExpect(jsonPath("$.items[0].productVariant").doesNotExist())
                .andExpect(jsonPath("$.items[0].product").doesNotExist())
                .andExpect(jsonPath("$.items[0].cart").doesNotExist())
                .andExpect(jsonPath("$.items[0].order").doesNotExist())
                .andReturn();

        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        createdOrderIds.add(orderId);
        transactionTemplate.executeWithoutResult(status -> {
            orderDeliveryAddressRepository.findByOrderId(orderId)
                    .ifPresent(delivery -> createdDeliveryAddressIds.add(delivery.getId()));
        });

        assertResponseItemHasOnlyCustomerFields(result);
        assertEquals(6, currentStock(variant));
        assertEquals(5, currentStock(otherVariant));
        assertEquals(0, cartItemCount(customerCart.getId()));
        assertEquals(1, cartItemCount(otherCart.getId()));
        assertEquals(1, orderCountForUser(customer));
        assertEquals(0, orderCountForUser(customer2));
    }

    @Test
    void emptyPersistedCart_returnsConflictWithExistingErrorShape() throws Exception {
        createCart(customer);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cart is empty"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(0, orderCountForUser(customer));
    }

    @Test
    void noCart_returnsSameConflictAsEmptyCartAndCreatesNoOrder() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cart is empty"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertFalse(cartRepository.findByUserId(customer.getId()).isPresent());
        assertEquals(0, orderCountForUser(customer));
    }

    @Test
    void missingToken_returnsUnauthorizedJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 4, "20.00", "S", "Blue");
        Cart cart = addCartItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(4, currentStock(variant));
        assertEquals(1, cartItemCount(cart.getId()));
        assertEquals(0, orderCountForUser(customer));
    }

    @Test
    void invalidBearerToken_returnsUnauthorizedJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 4, "20.00", "S", "Red");
        Cart cart = addCartItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(4, currentStock(variant));
        assertEquals(1, cartItemCount(cart.getId()));
        assertEquals(0, orderCountForUser(customer));
    }

    @Test
    void administratorToken_returnsForbiddenJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 4, "20.00", "XL", "White");
        Cart cart = addCartItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(4, currentStock(variant));
        assertEquals(1, cartItemCount(cart.getId()));
        assertEquals(0, orderCountForUser(customer));
        assertEquals(0, orderCountForUser(admin));
    }

    @Test
    void customerIsolation_ignoresClientBodyAndUsesJwtPrincipalOnly() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 7, "14.00", "M", "Silver");
        Cart customerCart = addCartItem(customer, variant, 2);

        String maliciousBody = """
                {"userId":%d,"cartId":%d,"variantId":%d,"quantity":2,"role":"ROLE_ADMIN"}
                """.formatted(customer.getId(), customerCart.getId(), variant.getId());

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cart is empty"));

        assertEquals(7, currentStock(variant));
        assertEquals(1, cartItemCount(customerCart.getId()));
        assertEquals(0, orderCountForUser(customer));
        assertEquals(0, orderCountForUser(customer2));

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andReturn();

        createdOrderIds.add(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong());
        assertEquals(5, currentStock(variant));
        assertEquals(0, cartItemCount(customerCart.getId()));
        assertEquals(1, orderCountForUser(customer));
    }

    @Test
    void repeatedCheckoutAfterSuccess_returnsConflictAndDoesNotCreateSecondOrderOrDeductStock() throws Exception {
        ProductVariant variant = createVariant(activeProduct, 3, "10.00", "S", "Purple");
        addCartItem(customer, variant, 1);

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andReturn();
        createdOrderIds.add(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong());

        assertEquals(2, currentStock(variant));
        assertEquals(1, orderCountForUser(customer));

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cart is empty"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(2, currentStock(variant));
        assertEquals(1, orderCountForUser(customer));
    }

    @Test
    void inactiveProductCheckout_returnsConflictWithoutOrderStockOrCartMutation() throws Exception {
        ProductVariant variant = createVariant(inactiveProduct, 6, "25.00", "M", "Orange");
        Cart cart = addCartItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cart contains unavailable items"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(6, currentStock(variant));
        assertEquals(1, cartItemCount(cart.getId()));
        assertEquals(0, orderCountForUser(customer));
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("Checkout");
        user.setLastName(label);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);
        createdUserIds.add(user.getId());

        CustomerAddress address = new CustomerAddress();
        address.setUser(user);
        address.setRecipientName(user.getFirstName());
        address.setPhoneNumber("12345");
        address.setAddressLine1("Line1");
        address.setCity("City");
        address.setCountry("USA");
        address.setIsDefault(true);
        address = customerAddressRepository.save(address);
        createdAddressIds.add(address.getId());
        return user;
    }

    private Product createProduct(boolean active) {
        Product product = new Product();
        product.setCategory(category);
        product.setName("Checkout Product " + UUID.randomUUID());
        product.setImageUrl("https://example.test/images/" + UUID.randomUUID() + ".jpg");
        product.setActive(active);
        product = productRepository.save(product);
        createdProductIds.add(product.getId());
        return product;
    }

    private ProductVariant createVariant(Product product, int stock, String price, String size, String color) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("SKU-CHECKOUT-" + UUID.randomUUID());
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());
        return cart;
    }

    private Cart addCartItem(User user, ProductVariant variant, int quantity) {
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> createCart(user));
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
        return cart;
    }

    private int currentStock(ProductVariant variant) {
        return productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
    }

    private long orderCountForUser(User user) {
        return transactionTemplate.execute(status -> customerOrderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .count());
    }

    private long cartItemCount(Long cartId) {
        return transactionTemplate.execute(status -> cartItemRepository.findAll().stream()
                .filter(item -> item.getCart().getId().equals(cartId))
                .count());
    }

    private void assertResponseItemHasOnlyCustomerFields(MvcResult result) throws Exception {
        JsonNode item = objectMapper.readTree(result.getResponse().getContentAsString()).get("items").get(0);
        Set<String> actualFields = new HashSet<>();
        item.fieldNames().forEachRemaining(actualFields::add);

        assertEquals(Set.of(
                "productName",
                "productImageUrl",
                "size",
                "color",
                "quantity",
                "unitPrice",
                "lineTotal"
        ), actualFields);
    }
}
