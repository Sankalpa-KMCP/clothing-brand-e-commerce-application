package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderDeliveryAddressDetailServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User customer1;
    private User customer2;
    private User admin;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdUserIds.clear();
        createdDeliveryAddressIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();

        customer1 = new User();
        customer1.setEmail("c1-" + UUID.randomUUID() + "@test.com");
        customer1.setFirstName("First");
        customer1.setLastName("Last");
        customer1.setPasswordHash("pass");
        customer1.setRole(customerRole);
        customer1.setActive(true);
        customer1 = userRepository.saveAndFlush(customer1);
        createdUserIds.add(customer1.getId());

        customer2 = new User();
        customer2.setEmail("c2-" + UUID.randomUUID() + "@test.com");
        customer2.setFirstName("First2");
        customer2.setLastName("Last2");
        customer2.setPasswordHash("pass");
        customer2.setRole(customerRole);
        customer2.setActive(true);
        customer2 = userRepository.saveAndFlush(customer2);
        createdUserIds.add(customer2.getId());

        admin = new User();
        admin.setEmail("a1-" + UUID.randomUUID() + "@test.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPasswordHash("pass");
        admin.setRole(adminRole);
        admin.setActive(true);
        admin = userRepository.saveAndFlush(admin);
        createdUserIds.add(admin.getId());
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(s -> {
            for (Long orderId : createdOrderIds) {
                orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).forEach(orderStatusHistoryRepository::delete);
            }
            for (Long deliveryId : createdDeliveryAddressIds) {
                if (orderDeliveryAddressRepository.existsById(deliveryId)) {
                    orderDeliveryAddressRepository.deleteById(deliveryId);
                }
            }
            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    customerOrderRepository.deleteById(orderId);
                });
            }
            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
        });

        transactionTemplate.executeWithoutResult(s -> {
            for (Long deliveryId : createdDeliveryAddressIds) {
                assertTrue(orderDeliveryAddressRepository.findById(deliveryId).isEmpty(), "Delivery leak");
            }
            for (Long orderId : createdOrderIds) {
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak");
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "User leak");
            }
        });
    }

    private CustomerOrder createOrder(User user) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);
        order = customerOrderRepository.saveAndFlush(order);
        createdOrderIds.add(order.getId());
        return order;
    }

    private OrderDeliveryAddress createSnapshot(CustomerOrder order) {
        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                "Recipient " + UUID.randomUUID(),
                "1234567890",
                "Line 1",
                "Line 2",
                "City",
                "Region",
                "12345",
                "Country"
        );
        deliveryAddress = orderDeliveryAddressRepository.saveAndFlush(deliveryAddress);
        createdDeliveryAddressIds.add(deliveryAddress.getId());
        return deliveryAddress;
    }

    @Test
    void customerDetail_withSnapshot_returnsSafeSnapshot() {
        CustomerOrder order = createOrder(customer1);
        OrderDeliveryAddress snap = createSnapshot(order);

        OrderDetailResponseDto dto = orderService.getMyOrder(customer1.getId(), order.getId());

        assertNotNull(dto.deliveryAddress());
        assertEquals(snap.getRecipientName(), dto.deliveryAddress().recipientName());
        assertEquals(snap.getPhoneNumber(), dto.deliveryAddress().phoneNumber());
        assertEquals(snap.getAddressLine1(), dto.deliveryAddress().addressLine1());
        assertEquals(snap.getAddressLine2(), dto.deliveryAddress().addressLine2());
        assertEquals(snap.getCity(), dto.deliveryAddress().city());
        assertEquals(snap.getRegion(), dto.deliveryAddress().region());
        assertEquals(snap.getPostalCode(), dto.deliveryAddress().postalCode());
        assertEquals(snap.getCountry(), dto.deliveryAddress().country());
    }

    @Test
    void customerDetail_withoutSnapshot_returnsNull() {
        CustomerOrder order = createOrder(customer1);

        OrderDetailResponseDto dto = orderService.getMyOrder(customer1.getId(), order.getId());
        assertNull(dto.deliveryAddress());
    }

    @Test
    void adminDetail_withSnapshot_returnsSafeSnapshot() {
        CustomerOrder order = createOrder(customer1);
        OrderDeliveryAddress snap = createSnapshot(order);

        OrderDetailResponseDto dto = orderService.getAdminOrder(order.getId());

        assertNotNull(dto.deliveryAddress());
        assertEquals(snap.getRecipientName(), dto.deliveryAddress().recipientName());
    }

    @Test
    void adminDetail_withoutSnapshot_returnsNull() {
        CustomerOrder order = createOrder(customer1);

        OrderDetailResponseDto dto = orderService.getAdminOrder(order.getId());
        assertNull(dto.deliveryAddress());
    }

    @Test
    void foreignOrder_returns404() {
        CustomerOrder order = createOrder(customer2);
        createSnapshot(order);

        assertThrows(ResourceNotFoundException.class, () -> orderService.getMyOrder(customer1.getId(), order.getId()));
    }
}
