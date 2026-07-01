package com.clothingbrand.ecommerce.domain.address;

import com.clothingbrand.ecommerce.domain.order.CustomerOrder;
import com.clothingbrand.ecommerce.domain.order.CustomerOrderRepository;
import com.clothingbrand.ecommerce.domain.order.OrderDeliveryAddress;
import com.clothingbrand.ecommerce.domain.order.OrderDeliveryAddressRepository;
import com.clothingbrand.ecommerce.domain.order.OrderStatus;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AddressPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdUserIds.clear();
        createdAddressIds.clear();
        createdOrderIds.clear();
        createdDeliveryAddressIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        testUser = createUser(customerRole, "address-test");
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (Long deliveryId : createdDeliveryAddressIds) {
                if (orderDeliveryAddressRepository.existsById(deliveryId)) {
                    orderDeliveryAddressRepository.deleteById(deliveryId);
                }
            }

            for (Long orderId : createdOrderIds) {
                if (customerOrderRepository.existsById(orderId)) {
                    customerOrderRepository.deleteById(orderId);
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
            for (Long deliveryId : createdDeliveryAddressIds) {
                assertTrue(orderDeliveryAddressRepository.findById(deliveryId).isEmpty(), "Delivery snapshot leak: " + deliveryId);
            }
            for (Long orderId : createdOrderIds) {
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak: " + orderId);
            }
            for (Long addressId : createdAddressIds) {
                assertTrue(customerAddressRepository.findById(addressId).isEmpty(), "Address leak: " + addressId);
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "User leak: " + userId);
            }
        });
    }

    @Test
    void customerAddressPersistsWithRequiredAndOptionalFields() {
        CustomerAddress address = new CustomerAddress();
        address.setUser(testUser);
        address.setLabel("Home");
        address.setRecipientName("Test Recipient");
        address.setPhoneNumber("1234567890");
        address.setAddressLine1("123 Main St");
        address.setAddressLine2("Apt 4B");
        address.setCity("Test City");
        address.setRegion("Test Region");
        address.setPostalCode("12345");
        address.setCountry("Test Country");
        address.setIsDefault(true);

        CustomerAddress saved = customerAddressRepository.saveAndFlush(address);
        createdAddressIds.add(saved.getId());

        assertNotNull(saved.getId());
        assertEquals("Home", saved.getLabel());
        assertEquals("Test Recipient", saved.getRecipientName());
        assertEquals("Apt 4B", saved.getAddressLine2());
        assertTrue(saved.getIsDefault());
    }

    @Test
    void schemaPreventsTwoDefaultAddressesForSameCustomer() {
        CustomerAddress address1 = new CustomerAddress();
        address1.setUser(testUser);
        address1.setRecipientName("Test Recipient 1");
        address1.setPhoneNumber("1234567890");
        address1.setAddressLine1("123 Main St");
        address1.setCity("Test City");
        address1.setCountry("Test Country");
        address1.setIsDefault(true);
        CustomerAddress saved1 = customerAddressRepository.saveAndFlush(address1);
        createdAddressIds.add(saved1.getId());

        CustomerAddress address2 = new CustomerAddress();
        address2.setUser(testUser);
        address2.setRecipientName("Test Recipient 2");
        address2.setPhoneNumber("0987654321");
        address2.setAddressLine1("456 Other St");
        address2.setCity("Other City");
        address2.setCountry("Other Country");
        address2.setIsDefault(true);

        assertThrows(DataIntegrityViolationException.class, () -> customerAddressRepository.saveAndFlush(address2));
    }

    @Test
    void nonDefaultAddressRemainsAllowedWhenCustomerAlreadyHasDefaultAddress() {
        CustomerAddress address1 = new CustomerAddress();
        address1.setUser(testUser);
        address1.setRecipientName("Test Recipient 1");
        address1.setPhoneNumber("1234567890");
        address1.setAddressLine1("123 Main St");
        address1.setCity("Test City");
        address1.setCountry("Test Country");
        address1.setIsDefault(true);
        CustomerAddress saved1 = customerAddressRepository.saveAndFlush(address1);
        createdAddressIds.add(saved1.getId());

        CustomerAddress address2 = new CustomerAddress();
        address2.setUser(testUser);
        address2.setRecipientName("Test Recipient 2");
        address2.setPhoneNumber("0987654321");
        address2.setAddressLine1("456 Other St");
        address2.setCity("Other City");
        address2.setCountry("Other Country");
        address2.setIsDefault(false);
        CustomerAddress saved2 = customerAddressRepository.saveAndFlush(address2);
        createdAddressIds.add(saved2.getId());

        assertNotNull(saved2.getId());
    }

    @Test
    void immutableOrderDeliverySnapshotCanPersistForRealOrder() {
        CustomerOrder order = createOrder(testUser);

        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                "Delivery Recipient",
                "1112223333",
                "789 Delivery Rd",
                "Suite 100",
                "Delivery City",
                "Delivery Region",
                "98765",
                "Delivery Country"
        );
        OrderDeliveryAddress saved = orderDeliveryAddressRepository.saveAndFlush(deliveryAddress);
        createdDeliveryAddressIds.add(saved.getId());

        assertNotNull(saved.getId());
        assertEquals("Delivery Recipient", saved.getRecipientName());
        assertEquals("789 Delivery Rd", saved.getAddressLine1());
    }

    @Test
    void deliverySnapshotIsFoundByOwningOrderId() {
        CustomerOrder order = createOrder(testUser);

        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                "Delivery Recipient",
                "1112223333",
                "789 Delivery Rd",
                null,
                "Delivery City",
                null,
                null,
                "Delivery Country"
        );
        OrderDeliveryAddress saved = orderDeliveryAddressRepository.saveAndFlush(deliveryAddress);
        createdDeliveryAddressIds.add(saved.getId());

        Optional<OrderDeliveryAddress> found = orderDeliveryAddressRepository.findByOrderId(order.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void orderWithoutDeliverySnapshotRemainsValidAndReturnsNoResult() {
        CustomerOrder order = createOrder(testUser);

        Optional<OrderDeliveryAddress> found = orderDeliveryAddressRepository.findByOrderId(order.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void deletingSavedAddressDoesNotAlterDeliverySnapshot() {
        // Create saved address
        CustomerAddress address = new CustomerAddress();
        address.setUser(testUser);
        address.setRecipientName("Shared Recipient");
        address.setPhoneNumber("5551234567");
        address.setAddressLine1("Shared St");
        address.setCity("Shared City");
        address.setCountry("Shared Country");
        address.setIsDefault(true);
        CustomerAddress savedAddress = customerAddressRepository.saveAndFlush(address);
        createdAddressIds.add(savedAddress.getId());

        // Create order and snapshot copying data
        CustomerOrder order = createOrder(testUser);
        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                savedAddress.getRecipientName(),
                savedAddress.getPhoneNumber(),
                savedAddress.getAddressLine1(),
                savedAddress.getAddressLine2(),
                savedAddress.getCity(),
                savedAddress.getRegion(),
                savedAddress.getPostalCode(),
                savedAddress.getCountry()
        );
        OrderDeliveryAddress savedDelivery = orderDeliveryAddressRepository.saveAndFlush(deliveryAddress);
        createdDeliveryAddressIds.add(savedDelivery.getId());

        // Delete saved address
        customerAddressRepository.deleteById(savedAddress.getId());
        createdAddressIds.remove(savedAddress.getId());
        customerAddressRepository.flush();

        // Verify snapshot still exists
        Optional<OrderDeliveryAddress> found = orderDeliveryAddressRepository.findByOrderId(order.getId());
        assertTrue(found.isPresent());
        assertEquals("Shared Recipient", found.get().getRecipientName());
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("Address");
        user.setLastName(label);
        user.setPasswordHash("password");
        user.setRole(role);
        user.setActive(true);
        user = userRepository.saveAndFlush(user);
        createdUserIds.add(user.getId());
        return user;
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
}
