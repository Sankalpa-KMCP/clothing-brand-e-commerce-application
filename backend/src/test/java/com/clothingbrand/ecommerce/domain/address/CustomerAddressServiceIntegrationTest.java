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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CustomerAddressServiceIntegrationTest {

    @Autowired
    private CustomerAddressService customerAddressService;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser1;
    private User testUser2;

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
        testUser1 = createUser(customerRole, "user1");
        testUser2 = createUser(customerRole, "user2");
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

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setActive(true);
        user = userRepository.saveAndFlush(user);
        createdUserIds.add(user.getId());
        return user;
    }

    private CustomerAddressRequestDto createValidRequest() {
        return new CustomerAddressRequestDto(
                "Home",
                "John Doe",
                "1234567890",
                "123 Main St",
                "Apt 4B",
                "Metropolis",
                "NY",
                "10001",
                "USA"
        );
    }

    @Test
    void createAddressPersistsAllApprovedFieldsAndStartsNonDefault() {
        CustomerAddressRequestDto req = createValidRequest();
        CustomerAddressResponseDto res = customerAddressService.createMyAddress(testUser1.getId(), req);
        createdAddressIds.add(res.id());

        assertNotNull(res.id());
        assertEquals(req.label(), res.label());
        assertEquals(req.recipientName(), res.recipientName());
        assertEquals(req.phoneNumber(), res.phoneNumber());
        assertEquals(req.addressLine1(), res.addressLine1());
        assertEquals(req.addressLine2(), res.addressLine2());
        assertEquals(req.city(), res.city());
        assertEquals(req.region(), res.region());
        assertEquals(req.postalCode(), res.postalCode());
        assertEquals(req.country(), res.country());
        assertFalse(res.isDefault());
    }

    @Test
    void listReturnsOnlyAuthenticatedCustomersAddressesInDeterministicOrder() {
        CustomerAddressResponseDto a1 = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(a1.id());
        CustomerAddressResponseDto a2 = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(a2.id());
        CustomerAddressResponseDto foreign = customerAddressService.createMyAddress(testUser2.getId(), createValidRequest());
        createdAddressIds.add(foreign.id());

        customerAddressService.setMyDefaultAddress(testUser1.getId(), a1.id());

        List<CustomerAddressResponseDto> list = customerAddressService.getMyAddresses(testUser1.getId());
        assertEquals(2, list.size());

        // Default first
        assertTrue(list.get(0).isDefault());
        assertEquals(a1.id(), list.get(0).id());

        assertFalse(list.get(1).isDefault());
        assertEquals(a2.id(), list.get(1).id());
    }

    @Test
    void emptyListReturnsNormalEmptyResult() {
        List<CustomerAddressResponseDto> list = customerAddressService.getMyAddresses(testUser1.getId());
        assertTrue(list.isEmpty());
    }

    @Test
    void readOwnAddressSucceeds() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());

        CustomerAddressResponseDto read = customerAddressService.getMyAddress(testUser1.getId(), created.id());
        assertEquals(created.id(), read.id());
        assertEquals("John Doe", read.recipientName());
    }

    @Test
    void updateOwnAddressUpdatesFieldsButPreservesDefaultState() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());

        customerAddressService.setMyDefaultAddress(testUser1.getId(), created.id());

        CustomerAddressRequestDto updateReq = new CustomerAddressRequestDto(
                "Work", "Jane Doe", "0987654321", "456 Side St", null, "Gotham", null, null, "USA"
        );
        CustomerAddressResponseDto updated = customerAddressService.updateMyAddress(testUser1.getId(), created.id(), updateReq);

        assertEquals("Work", updated.label());
        assertEquals("Jane Doe", updated.recipientName());
        assertTrue(updated.isDefault());
    }

    @Test
    void deleteOwnNonDefaultAddressSucceeds() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());

        customerAddressService.deleteMyAddress(testUser1.getId(), created.id());

        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.getMyAddress(testUser1.getId(), created.id()));
    }

    @Test
    void deleteDefaultAddressSucceedsAndLeavesNoDefault() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());
        customerAddressService.setMyDefaultAddress(testUser1.getId(), created.id());

        customerAddressService.deleteMyAddress(testUser1.getId(), created.id());

        List<CustomerAddressResponseDto> list = customerAddressService.getMyAddresses(testUser1.getId());
        assertTrue(list.isEmpty());
    }

    @Test
    void setDefaultSelectsExactlyOneOwnedDefault() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());

        CustomerAddressResponseDto def = customerAddressService.setMyDefaultAddress(testUser1.getId(), created.id());
        assertTrue(def.isDefault());
    }

    @Test
    void switchingDefaultClearsPriorDefaultAtomically() {
        CustomerAddressResponseDto a1 = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(a1.id());
        CustomerAddressResponseDto a2 = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(a2.id());

        customerAddressService.setMyDefaultAddress(testUser1.getId(), a1.id());
        customerAddressService.setMyDefaultAddress(testUser1.getId(), a2.id());

        List<CustomerAddressResponseDto> list = customerAddressService.getMyAddresses(testUser1.getId());
        assertEquals(2, list.size());

        long defaultCount = list.stream().filter(CustomerAddressResponseDto::isDefault).count();
        assertEquals(1, defaultCount);

        assertTrue(list.get(0).isDefault());
        assertEquals(a2.id(), list.get(0).id());
    }

    @Test
    void settingExistingDefaultIsIdempotent() {
        CustomerAddressResponseDto created = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(created.id());

        customerAddressService.setMyDefaultAddress(testUser1.getId(), created.id());
        CustomerAddressResponseDto def2 = customerAddressService.setMyDefaultAddress(testUser1.getId(), created.id());

        assertTrue(def2.isDefault());
    }

    @Test
    void foreignAddressReturnsNotFound() {
        CustomerAddressResponseDto foreign = customerAddressService.createMyAddress(testUser2.getId(), createValidRequest());
        createdAddressIds.add(foreign.id());

        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.getMyAddress(testUser1.getId(), foreign.id()));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.updateMyAddress(testUser1.getId(), foreign.id(), createValidRequest()));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.deleteMyAddress(testUser1.getId(), foreign.id()));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.setMyDefaultAddress(testUser1.getId(), foreign.id()));
    }

    @Test
    void missingAddressReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.getMyAddress(testUser1.getId(), -1L));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.updateMyAddress(testUser1.getId(), -1L, createValidRequest()));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.deleteMyAddress(testUser1.getId(), -1L));
        assertThrows(ResourceNotFoundException.class, () -> customerAddressService.setMyDefaultAddress(testUser1.getId(), -1L));
    }

    @Test
    void existingSnapshotRemainsUnchangedWhenRelatedAddressIsUpdatedOrDeleted() {
        CustomerAddressResponseDto a1 = customerAddressService.createMyAddress(testUser1.getId(), createValidRequest());
        createdAddressIds.add(a1.id());

        CustomerOrder order = new CustomerOrder();
        order.setUser(testUser1);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(BigDecimal.TEN);
        order.setTotal(BigDecimal.TEN);
        order = customerOrderRepository.saveAndFlush(order);
        createdOrderIds.add(order.getId());

        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                a1.recipientName(),
                a1.phoneNumber(),
                a1.addressLine1(),
                a1.addressLine2(),
                a1.city(),
                a1.region(),
                a1.postalCode(),
                a1.country()
        );
        deliveryAddress = orderDeliveryAddressRepository.saveAndFlush(deliveryAddress);
        createdDeliveryAddressIds.add(deliveryAddress.getId());

        // Update address
        CustomerAddressRequestDto updateReq = new CustomerAddressRequestDto(
                "Work", "Jane Doe", "0987654321", "456 Side St", null, "Gotham", null, null, "USA"
        );
        customerAddressService.updateMyAddress(testUser1.getId(), a1.id(), updateReq);

        // Snapshot unchanged
        Optional<OrderDeliveryAddress> snapOpt1 = orderDeliveryAddressRepository.findById(deliveryAddress.getId());
        assertTrue(snapOpt1.isPresent());
        assertEquals("John Doe", snapOpt1.get().getRecipientName());

        // Delete address
        customerAddressService.deleteMyAddress(testUser1.getId(), a1.id());

        // Snapshot unchanged
        Optional<OrderDeliveryAddress> snapOpt2 = orderDeliveryAddressRepository.findById(deliveryAddress.getId());
        assertTrue(snapOpt2.isPresent());
        assertEquals("John Doe", snapOpt2.get().getRecipientName());
    }
}
