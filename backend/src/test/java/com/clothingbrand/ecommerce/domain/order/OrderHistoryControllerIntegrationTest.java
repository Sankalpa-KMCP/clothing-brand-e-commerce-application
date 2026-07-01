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
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OrderHistoryControllerIntegrationTest {

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
        customer = createUser(customerRole, "history-http-a");
        customer2 = createUser(customerRole, "history-http-b");
        admin = createUser(adminRole, "history-http-admin");

        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        category = new Category();
        category.setName("History-Http-Cat-" + UUID.randomUUID());
        category = categoryRepository.save(category);
        createdCategoryIds.add(category.getId());

        product = createProduct("History Http Product");
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
    void validCustomerList_returnsSafePageContractWithOnlyCurrentCustomerOrders() throws Exception {
        ProductVariant variantA = createVariant(product, "ListA", 8, "10.00");
        ProductVariant variantB = createVariant(product, "ListB", 8, "12.00");
        CustomerOrder orderA = createOrder(customer, variantA, "HTTP Snapshot A", "a.jpg", "S", "Black", 1, "10.00");
        CustomerOrder orderB = createOrder(customer2, variantB, "HTTP Snapshot B", "b.jpg", "M", "Blue", 1, "12.00");

        MvcResult result = mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderA.getId()))
                .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                .andExpect(jsonPath("$.content[0].subtotal").value(10.00))
                .andExpect(jsonPath("$.content[0].total").value(10.00))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].sku").doesNotExist())
                .andExpect(jsonPath("$.content[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.content[0].userId").doesNotExist())
                .andExpect(jsonPath("$.content[0].cartId").doesNotExist())
                .andExpect(jsonPath("$.content[0].originalProductId").doesNotExist())
                .andExpect(jsonPath("$.content[0].originalVariantId").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andReturn();

        assertJsonFields(result, "", "content", "page", "size", "totalElements", "totalPages", "first", "last");
        assertJsonFields(result, "/content/0", "id", "status", "subtotal", "total", "createdAt");
        assertEquals(1, orderCountForUser(customer));
        assertEquals(1, orderCountForUser(customer2));
        assertTrue(customerOrderRepository.existsById(orderB.getId()));
    }

    @Test
    void customerPagination_usesPageAndSizeMetadataAndIgnoresClientSort() throws Exception {
        ProductVariant variant = createVariant(product, "Page", 8, "10.00");
        List<Long> orderIds = createOrdersInOneTransaction(customer, variant, 3);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "id,asc")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(orderIds.get(2)))
                .andExpect(jsonPath("$.content[1].id").value(orderIds.get(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderIds.get(0)))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void validCustomerDetail_returnsStoredSnapshotWithoutSensitiveJson() throws Exception {
        ProductVariant variant = createVariant(product, "Detail", 8, "15.00");
        CustomerOrder order = createOrder(customer, variant, "HTTP Detail Snapshot", "detail.jpg", "L", "Green", 2, "15.00");

        MvcResult result = mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.subtotal").value(30.00))
                .andExpect(jsonPath("$.total").value(30.00))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("HTTP Detail Snapshot"))
                .andExpect(jsonPath("$.items[0].productImageUrl").value("detail.jpg"))
                .andExpect(jsonPath("$.items[0].size").value("L"))
                .andExpect(jsonPath("$.items[0].color").value("Green"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(15.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(30.00))
                .andExpect(jsonPath("$.statusHistory").isArray())
                .andExpect(jsonPath("$.statusHistory").isEmpty())
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.originalProductId").doesNotExist())
                .andExpect(jsonPath("$.originalVariantId").doesNotExist())
                .andExpect(jsonPath("$.actorUserId").doesNotExist())
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.items[0].stockQuantity").doesNotExist())
                .andExpect(jsonPath("$.items[0].userId").doesNotExist())
                .andExpect(jsonPath("$.items[0].cartId").doesNotExist())
                .andExpect(jsonPath("$.items[0].originalProductId").doesNotExist())
                .andExpect(jsonPath("$.items[0].originalVariantId").doesNotExist())
                .andExpect(jsonPath("$.items[0].order").doesNotExist())
                .andExpect(jsonPath("$.items[0].product").doesNotExist())
                .andExpect(jsonPath("$.items[0].productVariant").doesNotExist())
                .andReturn();

        assertJsonFields(result, "", "id", "status", "subtotal", "total", "createdAt", "items", "statusHistory");
        assertJsonFields(result, "/items/0", "productName", "productImageUrl", "size", "color", "quantity", "unitPrice", "lineTotal");
    }

    @Test
    void emptyHistory_returnsOkEmptyPage() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void foreignOrder_returnsNonRevealingNotFound() throws Exception {
        ProductVariant variant = createVariant(product, "Foreign", 8, "15.00");
        CustomerOrder order = createOrder(customer, variant, "Foreign Snapshot", "foreign.jpg", "S", "Red", 1, "15.00");

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingOrder_returnsSameNotFoundContractAsForeignOrder() throws Exception {
        mockMvc.perform(get("/api/orders/999999999")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void missingToken_returnsUnauthorizedJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(product, "MissingToken", 5, "20.00");
        CustomerOrder order = createOrder(customer, variant, "Auth Snapshot", "auth.jpg", "M", "Black", 1, "20.00");
        int stockBefore = variantStock(variant);
        long orderCountBefore = customerOrderRepository.count();

        mockMvc.perform(get("/api/orders/" + order.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(stockBefore, variantStock(variant));
        assertEquals(orderCountBefore, customerOrderRepository.count());
    }

    @Test
    void invalidBearerToken_returnsUnauthorizedJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(product, "InvalidToken", 5, "20.00");
        CustomerOrder order = createOrder(customer, variant, "Invalid Snapshot", "invalid.jpg", "M", "Blue", 1, "20.00");
        long orderCountBefore = customerOrderRepository.count();

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(5, variantStock(variant));
        assertEquals(orderCountBefore, customerOrderRepository.count());
    }

    @Test
    void adminToken_returnsForbiddenJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(product, "Admin", 5, "20.00");
        CustomerOrder order = createOrder(customer, variant, "Admin Snapshot", "admin.jpg", "XL", "White", 1, "20.00");
        long orderCountBefore = customerOrderRepository.count();

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(5, variantStock(variant));
        assertEquals(orderCountBefore, customerOrderRepository.count());
    }

    @Test
    void invalidPagination_returnsBadRequestJsonAndDoesNotMutate() throws Exception {
        ProductVariant variant = createVariant(product, "BadPage", 5, "20.00");
        createOrder(customer, variant, "Bad Page Snapshot", "bad-page.jpg", "S", "Gray", 1, "20.00");
        long orderCountBefore = customerOrderRepository.count();

        mockMvc.perform(get("/api/orders")
                        .param("page", "-1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Page index must not be negative"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/orders")
                        .param("size", "0")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 50"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/orders")
                        .param("size", "51")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 50"));

        assertEquals(orderCountBefore, customerOrderRepository.count());
        assertEquals(5, variantStock(variant));
    }

    @Test
    void checkoutEndpointRegression_remainsCustomerOnlyAndCoexistsWithHistoryRoutes() throws Exception {
        ProductVariant variant = createVariant(product, "CheckoutRegression", 4, "10.00");
        Cart cart = createCartWithItem(customer, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(4, variantStock(variant));
        assertEquals(1, cartItemCount(cart.getId()));

        MvcResult checkout = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        Long orderId = objectMapper.readTree(checkout.getResponse().getContentAsString()).get("id").asLong();
        createdOrderIds.add(orderId);
        assertEquals(3, variantStock(variant));

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items[0].sku").doesNotExist())
                .andExpect(jsonPath("$.statusHistory.length()").value(1))
                .andExpect(jsonPath("$.statusHistory[0].previousStatus").isEmpty())
                .andExpect(jsonPath("$.statusHistory[0].newStatus").value("PLACED"))
                .andExpect(jsonPath("$.statusHistory[0].actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.statusHistory[0].createdAt").exists())
                .andExpect(jsonPath("$.statusHistory[0].actorUserId").doesNotExist());
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("History");
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
        newProduct.setImageUrl("https://example.test/history-http/" + UUID.randomUUID() + ".jpg");
        newProduct.setActive(true);
        newProduct = productRepository.save(newProduct);
        createdProductIds.add(newProduct.getId());
        return newProduct;
    }

    private ProductVariant createVariant(Product targetProduct, String label, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(targetProduct);
        variant.setSku("SKU-HISTORY-HTTP-" + label + "-" + UUID.randomUUID());
        variant.setSize("SZ-" + label + "-" + shortId());
        variant.setColor("CL-" + label + "-" + shortId());
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private CustomerOrder createOrder(User owner,
                                      ProductVariant variant,
                                      String productName,
                                      String productImageUrl,
                                      String size,
                                      String color,
                                      int quantity,
                                      String unitPrice) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(owner);
        order.setStatus(OrderStatus.PLACED);
        BigDecimal price = new BigDecimal(unitPrice);
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
        order.setSubtotal(lineTotal);
        order.setTotal(lineTotal);
        order.addItem(createOrderItem(variant, productName, productImageUrl, size, color, quantity, price, lineTotal));

        order = customerOrderRepository.saveAndFlush(order);
        createdOrderIds.add(order.getId());
        order.getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
        return customerOrderRepository.findById(order.getId()).orElseThrow();
    }

    private List<Long> createOrdersInOneTransaction(User owner, ProductVariant variant, int count) {
        return transactionTemplate.execute(status -> {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                CustomerOrder order = new CustomerOrder();
                order.setUser(owner);
                order.setStatus(OrderStatus.PLACED);
                BigDecimal price = new BigDecimal("10.00").add(BigDecimal.valueOf(i));
                order.setSubtotal(price);
                order.setTotal(price);
                order.addItem(createOrderItem(
                        variant,
                        "HTTP Page Snapshot " + i,
                        "page-" + i + ".jpg",
                        "S",
                        "Black-" + i,
                        1,
                        price,
                        price
                ));
                order = customerOrderRepository.saveAndFlush(order);
                createdOrderIds.add(order.getId());
                order.getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
                ids.add(order.getId());
            }
            return ids;
        });
    }

    private OrderItem createOrderItem(ProductVariant variant,
                                      String productName,
                                      String productImageUrl,
                                      String size,
                                      String color,
                                      int quantity,
                                      BigDecimal unitPrice,
                                      BigDecimal lineTotal) {
        OrderItem item = new OrderItem();
        item.setOriginalProductId(variant.getProduct().getId());
        item.setOriginalVariantId(variant.getId());
        item.setProductName(productName);
        item.setProductImageUrl(productImageUrl);
        item.setSku("ORDER-HTTP-SNAPSHOT-SKU-" + UUID.randomUUID());
        item.setSize(size);
        item.setColor(color);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setLineTotal(lineTotal);
        return item;
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

    private long orderCountForUser(User user) {
        return customerOrderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .count();
    }

    private long cartItemCount(Long cartId) {
        return cartItemRepository.findAll().stream()
                .filter(item -> item.getCart().getId().equals(cartId))
                .count();
    }

    private void assertJsonFields(MvcResult result, String pointer, String... expectedFields) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        assertEquals(
                new java.util.HashSet<>(List.of(expectedFields)),
                new java.util.HashSet<>(fields)
        );
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
