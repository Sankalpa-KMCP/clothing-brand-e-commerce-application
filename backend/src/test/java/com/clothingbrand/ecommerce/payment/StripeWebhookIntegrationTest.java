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
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "app.payment.stripe.enabled=true",
    "app.payment.stripe.secret-key=sk_test_placeholder",
    "app.payment.stripe.webhook-secret=whsec_test_secret",
    "app.payment.stripe.currency=USD",
    "app.payment.stripe.redirect-base-url=http://localhost:5173"
})
public class StripeWebhookIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User customer;
    private CustomerOrder order;
    private ProductVariant variant;
    private String stripeSessionId;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<String> createdWebhookEventIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        clearDatabaseOrders();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        createdOrderIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();
        createdWebhookEventIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        customer = new User();
        customer.setEmail("webhook-test-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Stripe");
        customer.setLastName("Webhook");
        customer.setPasswordHash("password");
        customer.setRole(customerRole);
        customer.setActive(true);
        customer = userRepository.save(customer);
        createdUserIds.add(customer.getId());

        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.save(cat);
        createdCategoryIds.add(cat.getId());

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Webhook Variant Product");
        prod.setActive(true);
        prod = productRepository.save(prod);
        createdProductIds.add(prod.getId());

        variant = new ProductVariant();
        variant.setProduct(prod);
        variant.setSku("SKU-WEBHOOK-" + UUID.randomUUID());
        variant.setSize("L");
        variant.setColor("Orange");
        variant.setPrice(new BigDecimal("19.99"));
        variant.setStockQuantity(10);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());

        // Setup Cart with Item
        Cart cart = new Cart();
        cart.setUser(customer);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());

        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2);
        cartItem = cartItemRepository.save(cartItem);
        createdCartItemIds.add(cartItem.getId());

        // Setup pending payment order
        stripeSessionId = "sess_" + UUID.randomUUID();
        order = new CustomerOrder();
        order.setUser(customer);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(new BigDecimal("39.98"));
        order.setTotal(new BigDecimal("39.98"));
        order.setStripeSessionId(stripeSessionId);
        order.setReservationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));

        OrderItem orderItem = new OrderItem();
        orderItem.setOriginalProductId(prod.getId());
        orderItem.setOriginalVariantId(variant.getId());
        orderItem.setProductName("Webhook Variant Product");
        orderItem.setSku(variant.getSku());
        orderItem.setSize("L");
        orderItem.setColor("Orange");
        orderItem.setUnitPrice(variant.getPrice());
        orderItem.setQuantity(2);
        orderItem.setLineTotal(new BigDecimal("39.98"));
        order.addItem(orderItem);

        order = customerOrderRepository.save(order);
        createdOrderIds.add(order.getId());
    }

    @AfterEach
    void cleanup() {
        clearDatabaseOrders();
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
            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
            for (String eventId : createdWebhookEventIds) {
                if (webhookEventRepository.existsById(eventId)) {
                    webhookEventRepository.deleteById(eventId);
                }
            }
        });
    }

    private void clearDatabaseOrders() {
        transactionTemplate.executeWithoutResult(status -> {
            orderStatusHistoryRepository.deleteAll();
            orderDeliveryAddressRepository.deleteAll();
            orderItemRepository.deleteAll();
            customerOrderRepository.deleteAll();
        });
    }

    private String generateSignature(String payload, String secret) throws Exception {
        long timestamp = Webhook.Util.getTimeNow();
        String payloadWithTimestamp = String.format("%d.%s", timestamp, payload);
        
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        sha256Hmac.init(secretKey);
        
        byte[] rawHmac = sha256Hmac.doFinal(payloadWithTimestamp.getBytes("UTF-8"));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        String signature = hexString.toString();
        return String.format("t=%d,v1=%s", timestamp, signature);
    }

    @Test
    void testWebhookUnauthenticatedRequestsOnlyWorkWithValidSignature() throws Exception {
        String payload = "{}";
        // Missing signature -> 400
        mockMvc.perform(post("/api/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());

        // Invalid signature -> 400
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", "invalid-sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testWebhookMalformedBearerTokenFailsClosed() throws Exception {
        String payload = "{}";
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Authorization", "Bearer invalidtoken123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSuccessfulPaidCompletionWebhookTransitionsAndClearsCart() throws Exception {
        String eventId = "evt_paid_" + UUID.randomUUID();
        createdWebhookEventIds.add(eventId);

        String payload = "{"
                + "\"id\": \"" + eventId + "\","
                + "\"object\": \"event\","
                + "\"type\": \"checkout.session.completed\","
                + "\"data\": {"
                + "  \"object\": {"
                + "    \"id\": \"" + stripeSessionId + "\","
                + "    \"object\": \"checkout.session\","
                + "    \"payment_status\": \"paid\","
                + "    \"payment_intent\": \"pi_success_123\","
                + "    \"metadata\": {"
                + "      \"order_id\": \"" + order.getId() + "\""
                + "    }"
                + "  }"
                + "}"
                + "}";

        String signature = generateSignature(payload, "whsec_test_secret");

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Verify order state mutated correctly
        CustomerOrder updatedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PLACED, updatedOrder.getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, updatedOrder.getPaymentStatus());
        assertEquals("pi_success_123", updatedOrder.getStripePaymentIntentId());
        assertNull(updatedOrder.getReservationExpiresAt());

        // Verify System history recorded
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        assertFalse(history.isEmpty());
        OrderStatusHistory latest = history.get(history.size() - 1);
        assertEquals(OrderStatus.PLACED, latest.getNewStatus());
        assertEquals(OrderActorType.SYSTEM, latest.getActorType());

        // Verify Cart cleared
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
            assertTrue(cart.getItems().isEmpty());
        });
    }

    @Test
    void testDuplicateDeliveryIsSuccessfulNoOp() throws Exception {
        String eventId = "evt_dup_" + UUID.randomUUID();
        createdWebhookEventIds.add(eventId);

        String payload = "{"
                + "\"id\": \"" + eventId + "\","
                + "\"object\": \"event\","
                + "\"type\": \"checkout.session.completed\","
                + "\"data\": {"
                + "  \"object\": {"
                + "    \"id\": \"" + stripeSessionId + "\","
                + "    \"object\": \"checkout.session\","
                + "    \"payment_status\": \"paid\","
                + "    \"payment_intent\": \"pi_dup_123\","
                + "    \"metadata\": {"
                + "      \"order_id\": \"" + order.getId() + "\""
                + "    }"
                + "  }"
                + "}"
                + "}";

        String signature = generateSignature(payload, "whsec_test_secret");

        // 1st delivery
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // 2nd delivery (duplicate)
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Event ID should be stored in webhook_events
        assertTrue(webhookEventRepository.existsById(eventId));
    }

    @Test
    void testWebhookSessionExpiryRestoresStockOnce() throws Exception {
        String eventId = "evt_expire_" + UUID.randomUUID();
        createdWebhookEventIds.add(eventId);

        String payload = "{"
                + "\"id\": \"" + eventId + "\","
                + "\"object\": \"event\","
                + "\"type\": \"checkout.session.expired\","
                + "\"data\": {"
                + "  \"object\": {"
                + "    \"id\": \"" + stripeSessionId + "\","
                + "    \"object\": \"checkout.session\","
                + "    \"payment_status\": \"unpaid\","
                + "    \"metadata\": {"
                + "      \"order_id\": \"" + order.getId() + "\""
                + "    }"
                + "  }"
                + "}"
                + "}";

        String signature = generateSignature(payload, "whsec_test_secret");

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Verify order transitioned to CANCELLED and stock restored
        CustomerOrder updatedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());
        assertEquals(PaymentStatus.FAILED, updatedOrder.getPaymentStatus());

        // Stock restored from 10 to 12
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(12, updatedVariant.getStockQuantity());
    }

    @Test
    void testSessionOrderMismatchesRejectAction() throws Exception {
        String eventId = "evt_mismatch_" + UUID.randomUUID();
        createdWebhookEventIds.add(eventId);

        String payload = "{"
                + "\"id\": \"" + eventId + "\","
                + "\"object\": \"event\","
                + "\"type\": \"checkout.session.completed\","
                + "\"data\": {"
                + "  \"object\": {"
                + "    \"id\": \"some_other_session_id\","
                + "    \"object\": \"checkout.session\","
                + "    \"payment_status\": \"paid\","
                + "    \"metadata\": {"
                + "      \"order_id\": \"" + order.getId() + "\""
                + "    }"
                + "  }"
                + "}"
                + "}";

        String signature = generateSignature(payload, "whsec_test_secret");

        // Returns 500 (Internal Server Error) due to mismatched session ID validation
        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isInternalServerError());

        // Data must not mutate
        CustomerOrder unchangedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING_PAYMENT, unchangedOrder.getStatus());
    }

    @Test
    void testUnrelatedVerifiedEventsAcknowledgedWithoutMutation() throws Exception {
        String eventId = "evt_unrelated_" + UUID.randomUUID();
        createdWebhookEventIds.add(eventId);

        String payload = "{"
                + "\"id\": \"" + eventId + "\","
                + "\"object\": \"event\","
                + "\"type\": \"payment_intent.created\","
                + "\"data\": {"
                + "  \"object\": {"
                + "    \"id\": \"pi_unrelated\","
                + "    \"object\": \"payment_intent\""
                + "  }"
                + "}"
                + "}";

        String signature = generateSignature(payload, "whsec_test_secret");

        mockMvc.perform(post("/api/webhooks/stripe")
                .header("Stripe-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Data must not mutate
        CustomerOrder unchangedOrder = customerOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING_PAYMENT, unchangedOrder.getStatus());

        assertTrue(webhookEventRepository.existsById(eventId));
    }
}
