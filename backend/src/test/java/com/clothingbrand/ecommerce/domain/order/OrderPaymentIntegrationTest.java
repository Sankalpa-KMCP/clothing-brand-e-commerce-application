package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderPaymentIntegrationTest {

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<String> createdWebhookEventIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdUserIds.clear();
        createdWebhookEventIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        testUser = new User();
        testUser.setEmail("payment-test-" + UUID.randomUUID() + "@test.com");
        testUser.setFirstName("Payment");
        testUser.setLastName("Tester");
        testUser.setPasswordHash("password");
        testUser.setRole(customerRole);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);
        createdUserIds.add(testUser.getId());
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (Long orderId : createdOrderIds) {
                if (customerOrderRepository.existsById(orderId)) {
                    customerOrderRepository.deleteById(orderId);
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

    @Test
    void testPaymentAndReservationFieldsPersistence() {
        OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
        String sessionId = "sess_" + UUID.randomUUID();
        String paymentIntentId = "pi_" + UUID.randomUUID();

        CustomerOrder order = new CustomerOrder();
        order.setUser(testUser);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);
        order.setStripeSessionId(sessionId);
        order.setStripePaymentIntentId(paymentIntentId);
        order.setReservationExpiresAt(expiry);

        CustomerOrder savedOrder = customerOrderRepository.save(order);
        assertNotNull(savedOrder.getId());
        createdOrderIds.add(savedOrder.getId());

        CustomerOrder fetched = customerOrderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.PENDING_PAYMENT, fetched.getStatus());
        assertEquals(PaymentStatus.PENDING, fetched.getPaymentStatus());
        assertEquals(sessionId, fetched.getStripeSessionId());
        assertEquals(paymentIntentId, fetched.getStripePaymentIntentId());
        assertNotNull(fetched.getReservationExpiresAt());
    }

    @Test
    void testUniquenessConstraintsOnStripeSessionId() {
        String duplicateSessionId = "sess_duplicate_" + UUID.randomUUID();

        CustomerOrder order1 = new CustomerOrder();
        order1.setUser(testUser);
        order1.setStatus(OrderStatus.PLACED);
        order1.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order1.setSubtotal(BigDecimal.TEN);
        order1.setTotal(BigDecimal.TEN);
        order1.setStripeSessionId(duplicateSessionId);
        customerOrderRepository.save(order1);
        createdOrderIds.add(order1.getId());

        CustomerOrder order2 = new CustomerOrder();
        order2.setUser(testUser);
        order2.setStatus(OrderStatus.PLACED);
        order2.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order2.setSubtotal(BigDecimal.TEN);
        order2.setTotal(BigDecimal.TEN);
        order2.setStripeSessionId(duplicateSessionId);

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                customerOrderRepository.save(order2);
                return null;
            });
        });
    }

    @Test
    void testUniquenessConstraintsOnStripePaymentIntentId() {
        String duplicatePiId = "pi_duplicate_" + UUID.randomUUID();

        CustomerOrder order1 = new CustomerOrder();
        order1.setUser(testUser);
        order1.setStatus(OrderStatus.PLACED);
        order1.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order1.setSubtotal(BigDecimal.TEN);
        order1.setTotal(BigDecimal.TEN);
        order1.setStripePaymentIntentId(duplicatePiId);
        customerOrderRepository.save(order1);
        createdOrderIds.add(order1.getId());

        CustomerOrder order2 = new CustomerOrder();
        order2.setUser(testUser);
        order2.setStatus(OrderStatus.PLACED);
        order2.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order2.setSubtotal(BigDecimal.TEN);
        order2.setTotal(BigDecimal.TEN);
        order2.setStripePaymentIntentId(duplicatePiId);

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                customerOrderRepository.save(order2);
                return null;
            });
        });
    }

    @Test
    void testNullStripeIdentifiersAllowedMultipleTimes() {
        CustomerOrder order1 = new CustomerOrder();
        order1.setUser(testUser);
        order1.setStatus(OrderStatus.PLACED);
        order1.setPaymentStatus(PaymentStatus.NOT_APPLICABLE);
        order1.setSubtotal(BigDecimal.TEN);
        order1.setTotal(BigDecimal.TEN);
        order1.setStripeSessionId(null);
        order1.setStripePaymentIntentId(null);
        customerOrderRepository.save(order1);
        createdOrderIds.add(order1.getId());

        CustomerOrder order2 = new CustomerOrder();
        order2.setUser(testUser);
        order2.setStatus(OrderStatus.PLACED);
        order2.setPaymentStatus(PaymentStatus.NOT_APPLICABLE);
        order2.setSubtotal(BigDecimal.TEN);
        order2.setTotal(BigDecimal.TEN);
        order2.setStripeSessionId(null);
        order2.setStripePaymentIntentId(null);
        
        assertDoesNotThrow(() -> {
            customerOrderRepository.save(order2);
            createdOrderIds.add(order2.getId());
        });
    }

    @Test
    void testWebhookEventIdempotencyConstraints() {
        String eventId = "evt_" + UUID.randomUUID();
        WebhookEvent event1 = new WebhookEvent(eventId, "checkout.session.completed");
        webhookEventRepository.save(event1);
        createdWebhookEventIds.add(eventId);

        WebhookEvent event2 = new WebhookEvent(eventId, "checkout.session.completed");

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                webhookEventRepository.save(event2);
                return null;
            });
        });
    }

    @Test
    void testDefaultPaymentStatusForNewOrdersIsNotApplicable() {
        CustomerOrder order = new CustomerOrder();
        order.setUser(testUser);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);

        CustomerOrder saved = customerOrderRepository.save(order);
        createdOrderIds.add(saved.getId());

        assertEquals(PaymentStatus.NOT_APPLICABLE, saved.getPaymentStatus());
    }

    @Test
    void testHistoricalOrderSchemaIntegrity() {
        // Verify that newly created orders via legacy checkout get the correct default payment_status
        CustomerOrder order = new CustomerOrder();
        order.setUser(testUser);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);
        CustomerOrder saved = customerOrderRepository.save(order);
        createdOrderIds.add(saved.getId());

        String status = jdbcTemplate.queryForObject(
                "SELECT payment_status FROM orders WHERE id = ?",
                String.class,
                saved.getId()
        );
        assertEquals("NOT_APPLICABLE", status);
    }
}
