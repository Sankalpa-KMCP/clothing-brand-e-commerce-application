package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartItem;
import com.clothingbrand.ecommerce.domain.cart.CartItemRepository;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
import com.clothingbrand.ecommerce.domain.cart.CartService;
import com.clothingbrand.ecommerce.domain.cart.CartItemRequestDto;
import com.clothingbrand.ecommerce.domain.cart.CartItemUpdateRequestDto;
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
import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderReservationIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

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
    private TransactionTemplate transactionTemplate;

    private User customer;
    private CustomerAddress address;
    private Product testProduct;
    private ProductVariant testVariant;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdDeliveryAddressIds.clear();
        createdAddressIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        customer = new User();
        customer.setEmail("res-cust-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Reservation");
        customer.setLastName("Customer");
        customer.setPasswordHash("password");
        customer.setRole(customerRole);
        customer.setActive(true);
        customer = userRepository.save(customer);
        createdUserIds.add(customer.getId());

        address = new CustomerAddress();
        address.setUser(customer);
        address.setRecipientName("Reservation Customer");
        address.setPhoneNumber("1234567890");
        address.setAddressLine1("123 Reservation Lane");
        address.setCity("Metropolis");
        address.setCountry("USA");
        address.setIsDefault(true);
        address = customerAddressRepository.save(address);
        createdAddressIds.add(address.getId());

        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.save(cat);
        createdCategoryIds.add(cat.getId());

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Test Product");
        prod.setActive(true);
        testProduct = productRepository.save(prod);
        createdProductIds.add(testProduct.getId());

        testVariant = new ProductVariant();
        testVariant.setProduct(testProduct);
        testVariant.setSku("SKU-RES-" + UUID.randomUUID());
        testVariant.setSize("M");
        testVariant.setColor("Green");
        testVariant.setPrice(new BigDecimal("29.99"));
        testVariant.setStockQuantity(20);
        testVariant = productVariantRepository.save(testVariant);
        createdVariantIds.add(testVariant.getId());
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
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    orderDeliveryAddressRepository.findByOrderId(orderId)
                        .ifPresent(delivery -> orderDeliveryAddressRepository.delete(delivery));
                    order.getItems().clear();
                    customerOrderRepository.saveAndFlush(order);
                    customerOrderRepository.deleteById(orderId);
                    customerOrderRepository.flush();
                });
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

    private void addCartItem(User user, ProductVariant variant, int quantity) {
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart = cartRepository.save(newCart);
            createdCartIds.add(newCart.getId());
            return newCart;
        });

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
    }

    @Test
    void testV9UniqueConstraintActivePendingPaymentOnly() {
        CustomerOrder order1 = new CustomerOrder();
        order1.setUser(customer);
        order1.setStatus(OrderStatus.PENDING_PAYMENT);
        order1.setPaymentStatus(PaymentStatus.PENDING);
        order1.setSubtotal(BigDecimal.TEN);
        order1.setTotal(BigDecimal.TEN);
        order1.setReservationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        customerOrderRepository.save(order1);
        createdOrderIds.add(order1.getId());

        CustomerOrder order2 = new CustomerOrder();
        order2.setUser(customer);
        order2.setStatus(OrderStatus.PENDING_PAYMENT);
        order2.setPaymentStatus(PaymentStatus.PENDING);
        order2.setSubtotal(BigDecimal.TEN);
        order2.setTotal(BigDecimal.TEN);
        order2.setReservationExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionTemplate.execute(status -> {
                customerOrderRepository.save(order2);
                return null;
            });
        });
    }

    @Test
    void testSuccessfulReservationPersistsCorrectlyAndLeavesCartIntact() {
        addCartItem(customer, testVariant, 2);

        CustomerOrder order = orderService.reservePaymentSession(customer.getId(), address.getId());
        createdOrderIds.add(order.getId());

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertNotNull(order.getReservationExpiresAt());
        assertTrue(order.getReservationExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)));

        // Verify stock deducted
        ProductVariant updatedVariant = productVariantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(18, updatedVariant.getStockQuantity());

        // Verify cart is NOT cleared
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
            assertFalse(cart.getItems().isEmpty());
            assertEquals(2, cart.getItems().get(0).getQuantity());
        });
    }

    @Test
    void testReuseExistingActiveReservationOnRetry() {
        addCartItem(customer, testVariant, 2);

        CustomerOrder orderFirst = orderService.reservePaymentSession(customer.getId(), address.getId());
        createdOrderIds.add(orderFirst.getId());

        // Call reservePaymentSession again - should return the same order without deducting stock again
        CustomerOrder orderSecond = orderService.reservePaymentSession(customer.getId(), address.getId());

        assertEquals(orderFirst.getId(), orderSecond.getId());
        
        // Stock should still be 18 (deducted once)
        ProductVariant updatedVariant = productVariantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(18, updatedVariant.getStockQuantity());
    }

    @Test
    void testCartMutationsBlockedDuringActiveReservation() {
        addCartItem(customer, testVariant, 2);
        
        CustomerOrder order = orderService.reservePaymentSession(customer.getId(), address.getId());
        createdOrderIds.add(order.getId());

        // 1. Block customer add operation
        CartItemRequestDto addRequest = new CartItemRequestDto(testVariant.getId(), 1);
        ResourceConflictException exAdd = assertThrows(ResourceConflictException.class, () -> {
            cartService.addCartItem(customer.getId(), addRequest);
        });
        assertTrue(exAdd.getMessage().contains("Cannot modify cart"));

        // 2. Block customer update operation
        Long itemId = transactionTemplate.execute(status -> 
            cartRepository.findByUserId(customer.getId()).orElseThrow().getItems().get(0).getId()
        );
        CartItemUpdateRequestDto updateRequest = new CartItemUpdateRequestDto(3);
        ResourceConflictException exUpdate = assertThrows(ResourceConflictException.class, () -> {
            cartService.updateCartItemQuantity(customer.getId(), itemId, updateRequest);
        });
        assertTrue(exUpdate.getMessage().contains("Cannot modify cart"));

        // 3. Block customer remove operation
        ResourceConflictException exRemove = assertThrows(ResourceConflictException.class, () -> {
            cartService.removeCartItem(customer.getId(), itemId);
        });
        assertTrue(exRemove.getMessage().contains("Cannot modify cart"));
    }

    @Test
    void testNoRegressionInLegacyCheckout() {
        addCartItem(customer, testVariant, 2);

        OrderResponseDto response = orderService.checkout(customer.getId(), address.getId());
        createdOrderIds.add(response.id());

        assertEquals(OrderStatus.PLACED.name(), response.status());

        // Stock should be deducted
        ProductVariant updatedVariant = productVariantRepository.findById(testVariant.getId()).orElseThrow();
        assertEquals(18, updatedVariant.getStockQuantity());

        // Cart is cleared in legacy checkout
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
            assertTrue(cart.getItems().isEmpty());
        });
    }

    @Test
    void testNoReservationCreatedOnValidationFailures() {
        // Test empty cart throws exception
        assertThrows(ResourceConflictException.class, () -> {
            orderService.reservePaymentSession(customer.getId(), address.getId());
        });

        // Test unavailable stock throws exception
        addCartItem(customer, testVariant, 50); // Variant has 20 in stock
        assertThrows(ResourceConflictException.class, () -> {
            orderService.reservePaymentSession(customer.getId(), address.getId());
        });
    }
}
