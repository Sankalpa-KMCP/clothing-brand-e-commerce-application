package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.user.Role;
import com.clothingbrand.ecommerce.domain.user.RoleName;
import com.clothingbrand.ecommerce.domain.user.RoleRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class OrderDeliveryAddressDetailControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;

    private User customer1;
    private User customer2;
    private User admin;
    private String customer1Token;
    private String customer2Token;
    private String adminToken;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdUserIds.clear();
        createdDeliveryAddressIds.clear();

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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

        customer1Token = jwtService.generateToken(new UserDetailsImpl(customer1));
        customer2Token = jwtService.generateToken(new UserDetailsImpl(customer2));
        adminToken = jwtService.generateToken(new UserDetailsImpl(admin));
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
    void customerOwnDetail_withSnapshot_returnsSafeSnapshot() throws Exception {
        CustomerOrder order = createOrder(customer1);
        OrderDeliveryAddress snap = createSnapshot(order);

        mockMvc.perform(get("/api/orders/" + order.getId())
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress.recipientName").value(snap.getRecipientName()))
                .andExpect(jsonPath("$.deliveryAddress.phoneNumber").value(snap.getPhoneNumber()))
                .andExpect(jsonPath("$.deliveryAddress.addressLine1").value(snap.getAddressLine1()))
                .andExpect(jsonPath("$.deliveryAddress.addressLine2").value(snap.getAddressLine2()))
                .andExpect(jsonPath("$.deliveryAddress.city").value(snap.getCity()))
                .andExpect(jsonPath("$.deliveryAddress.region").value(snap.getRegion()))
                .andExpect(jsonPath("$.deliveryAddress.postalCode").value(snap.getPostalCode()))
                .andExpect(jsonPath("$.deliveryAddress.country").value(snap.getCountry()))
                .andExpect(jsonPath("$.deliveryAddress.order").doesNotExist())
                .andExpect(jsonPath("$.deliveryAddress.createdAt").doesNotExist());
    }

    @Test
    void customerOwnDetail_withoutSnapshot_returnsNull() throws Exception {
        CustomerOrder order = createOrder(customer1);

        mockMvc.perform(get("/api/orders/" + order.getId())
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress").isEmpty());
    }

    @Test
    void customerForeignDetail_returns404() throws Exception {
        CustomerOrder order = createOrder(customer2);
        createSnapshot(order);

        mockMvc.perform(get("/api/orders/" + order.getId())
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.deliveryAddress").doesNotExist());
    }

    @Test
    void adminDetail_withSnapshot_returnsSafeSnapshot() throws Exception {
        CustomerOrder order = createOrder(customer1);
        OrderDeliveryAddress snap = createSnapshot(order);

        mockMvc.perform(get("/api/admin/orders/" + order.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress.recipientName").value(snap.getRecipientName()));
    }

    @Test
    void adminDetail_withoutSnapshot_returnsNull() throws Exception {
        CustomerOrder order = createOrder(customer1);

        mockMvc.perform(get("/api/admin/orders/" + order.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress").isEmpty());
    }

    @Test
    void adminDetail_withCustomerToken_returns403() throws Exception {
        CustomerOrder order = createOrder(customer1);
        createSnapshot(order);

        mockMvc.perform(get("/api/admin/orders/" + order.getId())
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerDetail_withAdminToken_returns403() throws Exception {
        CustomerOrder order = createOrder(customer1);
        createSnapshot(order);

        mockMvc.perform(get("/api/orders/" + order.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerOrderList_doesNotReturnDeliveryAddress() throws Exception {
        CustomerOrder order = createOrder(customer1);
        createSnapshot(order);

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryAddress").doesNotExist());
    }

    @Test
    void adminOrderList_doesNotReturnDeliveryAddress() throws Exception {
        CustomerOrder order = createOrder(customer1);
        createSnapshot(order);

        mockMvc.perform(get("/api/admin/orders")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deliveryAddress").doesNotExist());
    }
}
