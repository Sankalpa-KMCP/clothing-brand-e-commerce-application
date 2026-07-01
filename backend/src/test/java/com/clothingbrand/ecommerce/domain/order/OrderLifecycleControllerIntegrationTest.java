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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OrderLifecycleControllerIntegrationTest {

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
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

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
    private Product product;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdOrderItemIds = new ArrayList<>();
    private final List<Long> createdHistoryIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdOrderItemIds.clear();
        createdHistoryIds.clear();
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
        customer = createUser(customerRole, "lifecycle-http-a");
        customer2 = createUser(customerRole, "lifecycle-http-b");
        admin = createUser(adminRole, "lifecycle-http-admin");

        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        category = new Category();
        category.setName("Lifecycle-Http-Cat-" + UUID.randomUUID());
        category = categoryRepository.save(category);
        createdCategoryIds.add(category.getId());

        product = createProduct("Lifecycle Http Product");
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (CustomerOrder order : customerOrderRepository.findAll()) {
                if (createdUserIds.contains(order.getUser().getId()) && !createdOrderIds.contains(order.getId())) {
                    createdOrderIds.add(order.getId());
                }
            }

            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).forEach(history -> {
                        if (!createdHistoryIds.contains(history.getId())) {
                            createdHistoryIds.add(history.getId());
                        }
                    });
                    order.getItems().forEach(item -> {
                        if (!createdOrderItemIds.contains(item.getId())) {
                            createdOrderItemIds.add(item.getId());
                        }
                    });
                });
            }

            for (Long historyId : createdHistoryIds) {
                if (orderStatusHistoryRepository.existsById(historyId)) {
                    orderStatusHistoryRepository.deleteById(historyId);
                }
            }

            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    order.getItems().clear();
                    customerOrderRepository.saveAndFlush(order);
                    customerOrderRepository.deleteById(orderId);
                    customerOrderRepository.flush();
                });
            }

            for (Long orderItemId : createdOrderItemIds) {
                if (orderItemRepository.existsById(orderItemId)) {
                    orderItemRepository.deleteById(orderItemId);
                }
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

            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
        });

        transactionTemplate.executeWithoutResult(status -> {
            for (Long historyId : createdHistoryIds) {
                assertTrue(orderStatusHistoryRepository.findById(historyId).isEmpty(), "OrderStatusHistory leak: " + historyId);
            }
            for (Long orderId : createdOrderIds) {
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak: " + orderId);
            }
            for (Long orderItemId : createdOrderItemIds) {
                assertTrue(orderItemRepository.findById(orderItemId).isEmpty(), "OrderItem leak: " + orderItemId);
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
    void customerCancellationSuccessReturnsSafeDetailAndHistory() throws Exception {
        ProductVariant variant = createVariant(product, "Cancel", 5, "10.00");
        createCartWithItem(customer, variant, 2);
        Long orderId = checkout(customerToken);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.statusHistory[0].previousStatus").value(nullValue()))
                .andExpect(jsonPath("$.statusHistory[0].newStatus").value("PLACED"))
                .andExpect(jsonPath("$.statusHistory[0].actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.statusHistory[1].previousStatus").value("PLACED"))
                .andExpect(jsonPath("$.statusHistory[1].newStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.statusHistory[1].actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.statusHistory[1].createdAt").exists())
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.originalProductId").doesNotExist())
                .andExpect(jsonPath("$.originalVariantId").doesNotExist())
                .andExpect(jsonPath("$.actorUserId").doesNotExist())
                .andExpect(jsonPath("$.statusHistory[0].actorUserId").doesNotExist());

        assertEquals(5, variantStock(variant));
    }

    @Test
    void customerCancellationAuthorizationAndConflictUseExistingJsonBoundaries() throws Exception {
        ProductVariant variant = createVariant(product, "Auth", 6, "10.00");
        createCartWithItem(customer, variant, 1);
        Long orderId = checkout(customerToken);

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOrderStatusUpdateRequestDto(OrderStatus.PROCESSING))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertEquals(5, variantStock(variant));
    }

    @Test
    void adminListDetailAndStatusUpdateAreProtectedAndSafe() throws Exception {
        ProductVariant variant = createVariant(product, "Admin", 5, "10.00");
        createCartWithItem(customer, variant, 1);
        Long orderId = checkout(customerToken);

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").doesNotExist())
                .andExpect(jsonPath("$.content[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist());

        mockMvc.perform(get("/api/admin/orders")
                        .param("status", "PLACED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PLACED"));

        mockMvc.perform(get("/api/admin/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.statusHistory[0].newStatus").value("PLACED"))
                .andExpect(jsonPath("$.statusHistory[0].actorUserId").doesNotExist());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOrderStatusUpdateRequestDto(OrderStatus.PROCESSING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory[1].previousStatus").value("PLACED"))
                .andExpect(jsonPath("$.statusHistory[1].newStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory[1].actorType").value("ADMIN"));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminOrderStatusUpdateRequestDto(OrderStatus.PLACED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/orders/999999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void existingCustomerRoutesCoexistAndRemainCustomerOnly() throws Exception {
        ProductVariant variant = createVariant(product, "Coexist", 4, "10.00");
        createCartWithItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        Long orderId = checkout(customerToken);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId));

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.statusHistory[0].newStatus").value("PLACED"));

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    private Long checkout(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        createdOrderIds.add(orderId);
        customerOrderRepository.findByIdWithItems(orderId).orElseThrow()
                .getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
        orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
                .forEach(history -> createdHistoryIds.add(history.getId()));
        return orderId;
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("Lifecycle");
        user.setLastName(label);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);
        createdUserIds.add(user.getId());
        return user;
    }

    private Product createProduct(String label) {
        Product newProduct = new Product();
        newProduct.setCategory(category);
        newProduct.setName(label + " " + UUID.randomUUID());
        newProduct.setImageUrl("https://example.test/lifecycle-http/" + UUID.randomUUID() + ".jpg");
        newProduct.setActive(true);
        newProduct = productRepository.save(newProduct);
        createdProductIds.add(newProduct.getId());
        return newProduct;
    }

    private ProductVariant createVariant(Product targetProduct, String label, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(targetProduct);
        variant.setSku("SKU-LIFECYCLE-HTTP-" + label + "-" + UUID.randomUUID());
        variant.setSize("SZ-" + label + "-" + shortId());
        variant.setColor("CL-" + label + "-" + shortId());
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private Cart createCartWithItem(User owner, ProductVariant variant, int quantity) {
        Cart cart = new Cart();
        cart.setUser(owner);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
        return cart;
    }

    private int variantStock(ProductVariant variant) {
        return productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
