package com.clothingbrand.ecommerce.payment;

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
import com.clothingbrand.ecommerce.domain.order.*;
import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.payment.stripe.enabled=true",
    "app.payment.stripe.secret-key=sk_test_placeholder",
    "app.payment.stripe.webhook-secret=whsec_test_secret",
    "app.payment.stripe.currency=USD",
    "app.payment.stripe.redirect-base-url=http://localhost:5173",
    "app.payment.stripe.session-timeout-seconds=1800"
})
public class OrderCheckoutReserveIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StripeGateway stripeGateway;

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
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customer;
    private String customerToken;
    private User admin;
    private String adminToken;
    private CustomerAddress address;
    private ProductVariant variant;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();

    @BeforeEach
    void setup() throws Exception {
        createdOrderIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();
        createdAddressIds.clear();

        dateTimeProvider.reset();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        customer = new User();
        customer.setEmail("reserve-test-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Reserve");
        customer.setLastName("Tester");
        customer.setPasswordHash("password");
        customer.setRole(customerRole);
        customer.setActive(true);
        customer = userRepository.saveAndFlush(customer);
        createdUserIds.add(customer.getId());

        customerToken = jwtService.generateToken(new UserDetailsImpl(customer));

        admin = new User();
        admin.setEmail("reserve-admin-" + UUID.randomUUID() + "@test.com");
        admin.setFirstName("Reserve");
        admin.setLastName("Admin");
        admin.setPasswordHash("password");
        admin.setRole(adminRole);
        admin.setActive(true);
        admin = userRepository.saveAndFlush(admin);
        createdUserIds.add(admin.getId());

        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));

        address = new CustomerAddress();
        address.setUser(customer);
        address.setRecipientName("Recipient");
        address.setPhoneNumber("1234567890");
        address.setAddressLine1("123 Street");
        address.setCity("City");
        address.setRegion("Region");
        address.setPostalCode("12345");
        address.setCountry("USA");
        address.setIsDefault(true);
        address = customerAddressRepository.saveAndFlush(address);
        createdAddressIds.add(address.getId());

        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.saveAndFlush(cat);
        createdCategoryIds.add(cat.getId());

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Reserve Product");
        prod.setActive(true);
        prod = productRepository.saveAndFlush(prod);
        createdProductIds.add(prod.getId());

        variant = new ProductVariant();
        variant.setProduct(prod);
        variant.setSku("SKU-RESERVE-" + UUID.randomUUID());
        variant.setSize("M");
        variant.setColor("Green");
        variant.setPrice(new BigDecimal("25.00"));
        variant.setStockQuantity(10);
        variant = productVariantRepository.saveAndFlush(variant);
        createdVariantIds.add(variant.getId());

        Cart cart = new Cart();
        cart.setUser(customer);
        cart = cartRepository.saveAndFlush(cart);
        createdCartIds.add(cart.getId());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item = cartItemRepository.saveAndFlush(item);
        createdCartItemIds.add(item.getId());

        // Setup Stripe Mocks
        when(stripeGateway.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void cleanup() {
        dateTimeProvider.reset();

        cleanupCreatedOrders();
        transactionTemplate.executeWithoutResult(status -> {
            for (Long cartItemId : createdCartItemIds) {
                if (cartItemRepository.existsById(cartItemId)) {
                    cartItemRepository.deleteById(cartItemId);
                }
            }
            for (Long cartId : createdCartIds) {
                cartRepository.findById(cartId).ifPresent(c -> {
                    c.getItems().clear();
                    cartRepository.saveAndFlush(c);
                    cartRepository.deleteById(cartId);
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
    }

    private void cleanupCreatedOrders() {
        transactionTemplate.executeWithoutResult(status -> {
            java.util.LinkedHashSet<Long> orderIdsToDelete = new java.util.LinkedHashSet<>(createdOrderIds);
            for (Long userId : createdUserIds) {
                customerOrderRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 50))
                        .forEach(order -> orderIdsToDelete.add(order.getId()));
            }

            for (Long orderId : orderIdsToDelete) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId)
                            .forEach(orderStatusHistoryRepository::delete);
                    orderDeliveryAddressRepository.findByOrderId(orderId)
                            .ifPresent(orderDeliveryAddressRepository::delete);
                    order.getItems().clear();
                    customerOrderRepository.saveAndFlush(order);
                    customerOrderRepository.deleteById(orderId);
                });
            }
        });
    }

    @Test
    void testReserveRequiresCustomerAuthentication() throws Exception {
        CheckoutRequestDto requestDto = new CheckoutRequestDto(address.getId());

        mockMvc.perform(post("/api/orders/checkout/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/orders/checkout/reserve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        verify(stripeGateway, never()).createCheckoutSession(any(), any(), any(), anyInt());
    }

    @Test
    void testSuccessfulSessionResponseAndReservationReuse() throws Exception {
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test_session_id_123");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_session_id_123");

        when(stripeGateway.createCheckoutSession(any(), any(), any(), anyInt())).thenReturn(mockSession);

        CheckoutRequestDto requestDto = new CheckoutRequestDto(address.getId());

        // 1. First Call: Creates reservation, contacts Stripe, and returns redirect URL
        String content = mockMvc.perform(post("/api/orders/checkout/reserve")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.stripeCheckoutUrl").value("https://checkout.stripe.com/pay/cs_test_session_id_123"))
                .andExpect(jsonPath("$.reservationExpiresAt").exists())
                .andExpect(jsonPath("$.stripeSessionId").doesNotExist())
                .andExpect(jsonPath("$.paymentIntentId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"orderId\":\\s*(\\d+)").matcher(content);
        assertTrue(matcher.find());
        Long orderId = Long.parseLong(matcher.group(1));
        createdOrderIds.add(orderId);

        // Verify variant stock is deducted (10 - 2 = 8)
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(8, updatedVariant.getStockQuantity());

        // Verify order is created in PENDING_PAYMENT status
        CustomerOrder order = customerOrderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals("cs_test_session_id_123", order.getStripeSessionId());

        // 2. Second Call (Retry): Reuses existing active reservation, retrieves the session, and does NOT deduct stock again
        StripeClient mockStripeClient = mock(StripeClient.class);
        CheckoutService mockCheckoutService = mock(CheckoutService.class);
        SessionService mockSessionService = mock(SessionService.class);

        when(stripeGateway.getStripeClient()).thenReturn(mockStripeClient);
        when(mockStripeClient.checkout()).thenReturn(mockCheckoutService);
        when(mockCheckoutService.sessions()).thenReturn(mockSessionService);
        when(mockSessionService.retrieve("cs_test_session_id_123")).thenReturn(mockSession);

        mockMvc.perform(post("/api/orders/checkout/reserve")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.stripeCheckoutUrl").value("https://checkout.stripe.com/pay/cs_test_session_id_123"));

        // Verify stock is still 8 (not deducted twice)
        ProductVariant updatedVariant2 = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(8, updatedVariant2.getStockQuantity());
    }

    @Test
    void testRetryAfterAmbiguousStripeFailureReusesReservationWithoutExtraStockDeduction() throws Exception {
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test_retry_session");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_retry_session");

        when(stripeGateway.createCheckoutSession(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Ambiguous Stripe connection failure"))
                .thenReturn(mockSession);

        CheckoutRequestDto requestDto = new CheckoutRequestDto(address.getId());

        try {
            int statusCode = mockMvc.perform(post("/api/orders/checkout/reserve")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
            assertTrue(statusCode >= 500);
        } catch (Exception ignored) {
            // MockMvc may rethrow unhandled runtime exceptions instead of rendering a 5xx response.
        }

        CustomerOrder pendingOrder = customerOrderRepository
                .findActivePendingPaymentOrder(customer.getId(), dateTimeProvider.now())
                .orElseThrow();
        createdOrderIds.add(pendingOrder.getId());
        assertNull(pendingOrder.getStripeSessionId());

        ProductVariant afterFailure = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(8, afterFailure.getStockQuantity());

        mockMvc.perform(post("/api/orders/checkout/reserve")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(pendingOrder.getId()))
                .andExpect(jsonPath("$.stripeCheckoutUrl").value("https://checkout.stripe.com/pay/cs_test_retry_session"));

        ProductVariant afterRetry = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(8, afterRetry.getStockQuantity());

        org.mockito.ArgumentCaptor<CustomerOrder> orderCaptor = org.mockito.ArgumentCaptor.forClass(CustomerOrder.class);
        verify(stripeGateway, times(2)).createCheckoutSession(orderCaptor.capture(), any(), any(), anyInt());
        assertEquals(pendingOrder.getId(), orderCaptor.getAllValues().get(0).getId());
        assertEquals(pendingOrder.getId(), orderCaptor.getAllValues().get(1).getId());

        CustomerOrder savedOrder = customerOrderRepository.findById(pendingOrder.getId()).orElseThrow();
        assertEquals("cs_test_retry_session", savedOrder.getStripeSessionId());
    }

    @Test
    void testDisabledFeatureBehavior() throws Exception {
        when(stripeGateway.isEnabled()).thenReturn(false);

        CheckoutRequestDto requestDto = new CheckoutRequestDto(address.getId());

        mockMvc.perform(post("/api/orders/checkout/reserve")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stripe payment gateway is disabled."));
    }
}
