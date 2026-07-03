package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartItem;
import com.clothingbrand.ecommerce.domain.cart.CartItemRepository;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
import com.clothingbrand.ecommerce.domain.cart.CartService;
import com.clothingbrand.ecommerce.domain.cart.CartItemRequestDto;
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
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "app.payment.stripe.enabled=true",
    "app.payment.stripe.secret-key=sk_test_placeholder",
    "app.payment.stripe.webhook-secret=whsec_test_secret",
    "app.payment.stripe.currency=USD",
    "app.payment.stripe.redirect-base-url=http://localhost:5173",
    "app.payment.reservation.cleanup.enabled=true",
    "app.payment.reservation.cleanup.fixed-delay-ms=99999999"
})
public class PaymentReservationRecoveryIntegrationTest {

    @Autowired
    private PaymentReservationRecoveryService recoveryService;

    @Autowired
    private PaymentReservationRecoveryScheduler recoveryScheduler;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

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
    private CartService cartService;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User customer;
    private ProductVariant variant;
    private String stripeSessionId;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();

        dateTimeProvider.reset();
        clearDatabaseOrders();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        customer = new User();
        customer.setEmail("scheduler-test-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Scheduler");
        customer.setLastName("Tester");
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
        prod.setName("Scheduled Product");
        prod.setActive(true);
        prod = productRepository.save(prod);
        createdProductIds.add(prod.getId());

        variant = new ProductVariant();
        variant.setProduct(prod);
        variant.setSku("SKU-SCHED-" + UUID.randomUUID());
        variant.setSize("S");
        variant.setColor("Blue");
        variant.setPrice(new BigDecimal("10.00"));
        variant.setStockQuantity(15);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
    }

    @AfterEach
    void cleanup() {
        dateTimeProvider.reset();
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

    private CustomerOrder createPendingOrder(OffsetDateTime expiry, int quantity) {
        CustomerOrder o = new CustomerOrder();
        o.setUser(customer);
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setSubtotal(variant.getPrice().multiply(BigDecimal.valueOf(quantity)));
        o.setTotal(variant.getPrice().multiply(BigDecimal.valueOf(quantity)));
        o.setStripeSessionId("sess_" + UUID.randomUUID());
        o.setReservationExpiresAt(expiry);

        OrderItem orderItem = new OrderItem();
        orderItem.setOriginalProductId(variant.getProduct().getId());
        orderItem.setOriginalVariantId(variant.getId());
        orderItem.setProductName(variant.getProduct().getName());
        orderItem.setSku(variant.getSku());
        orderItem.setSize(variant.getSize());
        orderItem.setColor(variant.getColor());
        orderItem.setUnitPrice(variant.getPrice());
        orderItem.setQuantity(quantity);
        orderItem.setLineTotal(variant.getPrice().multiply(BigDecimal.valueOf(quantity)));
        o.addItem(orderItem);

        // Deduct variant stock manually for setup
        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        productVariantRepository.saveAndFlush(variant);

        CustomerOrder saved = customerOrderRepository.save(o);
        createdOrderIds.add(saved.getId());
        return saved;
    }

    private void createCartWithItem(int quantity) {
        Cart cart = new Cart();
        cart.setUser(customer);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
    }

    @Test
    void testExpiredPendingReservationIsCancelledStockRestoredAndCartUnlocked() {
        // Fix test time: 12:00:00
        Clock clock1 = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock1);

        // Create pending order expiring 10 minutes in the future (expires at 12:10:00)
        CustomerOrder expiredOrder = createPendingOrder(OffsetDateTime.now(clock1).plusMinutes(10), 2);
        createCartWithItem(2);

        // Verify cart mutation is blocked initially
        CartItemRequestDto addRequest = new CartItemRequestDto(variant.getId(), 1);
        assertThrows(ResourceConflictException.class, () -> {
            cartService.addCartItem(customer.getId(), addRequest);
        });

        // Advance clock to 12:15:00 (order is now expired)
        Clock clock2 = Clock.fixed(Instant.parse("2026-07-02T12:15:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock2);

        // Run recovery scheduler
        recoveryScheduler.cleanupExpiredReservations();

        // Verify order is CANCELLED and payment is FAILED
        CustomerOrder updatedOrder = customerOrderRepository.findById(expiredOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());
        assertEquals(PaymentStatus.FAILED, updatedOrder.getPaymentStatus());
        assertNull(updatedOrder.getReservationExpiresAt());

        // Verify stock is restored (15 - 2 + 2 = 15)
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(15, updatedVariant.getStockQuantity());

        // Verify cart mutation is now allowed (unlocked)
        assertDoesNotThrow(() -> {
            cartService.addCartItem(customer.getId(), addRequest);
        });

        // Verify history has CANCELLED transition
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(expiredOrder.getId());
        assertFalse(history.isEmpty());
        OrderStatusHistory latest = history.get(history.size() - 1);
        assertEquals(OrderStatus.CANCELLED, latest.getNewStatus());
        assertEquals(OrderActorType.SYSTEM, latest.getActorType());
    }

    @Test
    void testNonExpiredReservationIsUntouched() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock);

        // Expires 10 minutes in the future
        CustomerOrder activeOrder = createPendingOrder(OffsetDateTime.now(clock).plusMinutes(10), 2);

        // Run recovery scheduler
        recoveryScheduler.cleanupExpiredReservations();

        // Verify untouched
        CustomerOrder updatedOrder = customerOrderRepository.findById(activeOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING_PAYMENT, updatedOrder.getStatus());
        assertEquals(PaymentStatus.PENDING, updatedOrder.getPaymentStatus());

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(13, updatedVariant.getStockQuantity()); // 15 - 2 = 13
    }

    @Test
    void testPaidPlacedOrderIsUntouchedEvenIfOldExpiryTimestampExists() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock);

        CustomerOrder placedOrder = createPendingOrder(OffsetDateTime.now(clock).minusMinutes(10), 2);
        placedOrder.setStatus(OrderStatus.PLACED);
        placedOrder.setPaymentStatus(PaymentStatus.SUCCEEDED);
        customerOrderRepository.saveAndFlush(placedOrder);

        // Run recovery scheduler
        recoveryScheduler.cleanupExpiredReservations();

        // Verify untouched
        CustomerOrder updatedOrder = customerOrderRepository.findById(placedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.PLACED, updatedOrder.getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, updatedOrder.getPaymentStatus());

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(13, updatedVariant.getStockQuantity()); // 15 - 2 = 13
    }

    @Test
    void testRepeatedSchedulerRunsAreIdempotent() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock);

        CustomerOrder expiredOrder = createPendingOrder(OffsetDateTime.now(clock).minusMinutes(10), 2);

        int originalHistoryCount = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(expiredOrder.getId()).size();

        // Run 1st time
        recoveryScheduler.cleanupExpiredReservations();

        // Run 2nd time
        recoveryScheduler.cleanupExpiredReservations();

        // Verify stock is restored only once
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(15, updatedVariant.getStockQuantity());

        // Verify history has exactly one extra transition added
        int newHistoryCount = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(expiredOrder.getId()).size();
        assertEquals(originalHistoryCount + 1, newHistoryCount);
    }

    @Test
    void testConcurrencySafetyWhenWebhookCompletesFirst() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock);

        CustomerOrder expiredOrder = createPendingOrder(OffsetDateTime.now(clock).minusMinutes(10), 2);

        // 1. Simulate Webhook completes first (transitions order to PLACED)
        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder o = customerOrderRepository.findByIdForUpdate(expiredOrder.getId()).orElseThrow();
            o.setStatus(OrderStatus.PLACED);
            o.setPaymentStatus(PaymentStatus.SUCCEEDED);
            customerOrderRepository.save(o);
        });

        // 2. Scheduler runs
        recoveryScheduler.cleanupExpiredReservations();

        // Verify order remains PLACED, stock is NOT restored
        CustomerOrder finalOrder = customerOrderRepository.findById(expiredOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.PLACED, finalOrder.getStatus());
        assertEquals(PaymentStatus.SUCCEEDED, finalOrder.getPaymentStatus());

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(13, updatedVariant.getStockQuantity());
    }

    @Test
    void testOneProcessingFailureDoesNotPreventOtherReservationsFromBeingRecovered() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);
        dateTimeProvider.setClock(clock);

        // Create a failing order (by referencing an invalid variant in snap metadata, or by deleting the variant first)
        CustomerOrder failingOrder = createPendingOrder(OffsetDateTime.now(clock).minusMinutes(10), 2);
        
        // Delete variant to trigger dynamic constraint fail on adjustStock
        productVariantRepository.delete(variant);
        productVariantRepository.flush();

        // Create a second valid order for a different customer to avoid constraint violation
        User customer2 = new User();
        customer2.setEmail("scheduler-test2-" + UUID.randomUUID() + "@test.com");
        customer2.setFirstName("Scheduler2");
        customer2.setLastName("Tester2");
        customer2.setPasswordHash("password");
        customer2.setRole(roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow());
        customer2.setActive(true);
        customer2 = userRepository.save(customer2);
        createdUserIds.add(customer2.getId());

        ProductVariant variant2 = new ProductVariant();
        variant2.setProduct(variant.getProduct());
        variant2.setSku("SKU-SCHED-2-" + UUID.randomUUID());
        variant2.setSize("M");
        variant2.setColor("Red");
        variant2.setPrice(new BigDecimal("15.00"));
        variant2.setStockQuantity(10);
        variant2 = productVariantRepository.saveAndFlush(variant2);
        createdVariantIds.add(variant2.getId());

        CustomerOrder validOrder = new CustomerOrder();
        validOrder.setUser(customer2);
        validOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        validOrder.setPaymentStatus(PaymentStatus.PENDING);
        validOrder.setSubtotal(variant2.getPrice().multiply(BigDecimal.valueOf(2)));
        validOrder.setTotal(variant2.getPrice().multiply(BigDecimal.valueOf(2)));
        validOrder.setStripeSessionId("sess_valid_" + UUID.randomUUID());
        validOrder.setReservationExpiresAt(OffsetDateTime.now(clock).minusMinutes(10));

        OrderItem orderItem = new OrderItem();
        orderItem.setOriginalProductId(variant2.getProduct().getId());
        orderItem.setOriginalVariantId(variant2.getId());
        orderItem.setProductName(variant2.getProduct().getName());
        orderItem.setSku(variant2.getSku());
        orderItem.setSize(variant2.getSize());
        orderItem.setColor(variant2.getColor());
        orderItem.setUnitPrice(variant2.getPrice());
        orderItem.setQuantity(2);
        orderItem.setLineTotal(variant2.getPrice().multiply(BigDecimal.valueOf(2)));
        validOrder.addItem(orderItem);

        variant2.setStockQuantity(8);
        productVariantRepository.saveAndFlush(variant2);

        CustomerOrder savedValid = customerOrderRepository.save(validOrder);
        createdOrderIds.add(savedValid.getId());

        // Run recovery scheduler
        assertDoesNotThrow(() -> {
            recoveryScheduler.cleanupExpiredReservations();
        });

        // Verify validOrder is successfully cancelled and restored
        CustomerOrder updatedValid = customerOrderRepository.findById(savedValid.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedValid.getStatus());
        assertEquals(PaymentStatus.FAILED, updatedValid.getPaymentStatus());

        ProductVariant updatedVariant2 = productVariantRepository.findById(variant2.getId()).orElseThrow();
        assertEquals(10, updatedVariant2.getStockQuantity());
    }
}
