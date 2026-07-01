package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
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
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
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
public class OrderCheckoutDeliveryAddressServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

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
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User customer1;
    private User customer2;
    private Product testProduct;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdCartIds = new ArrayList<>();
    private final List<Long> createdCartItemIds = new ArrayList<>();
    private final List<Long> createdVariantIds = new ArrayList<>();
    private final List<Long> createdProductIds = new ArrayList<>();
    private final List<Long> createdCategoryIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        createdOrderIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();
        createdAddressIds.clear();
        createdDeliveryAddressIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

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

        Category cat = new Category();
        cat.setName("Cat-" + UUID.randomUUID());
        cat = categoryRepository.saveAndFlush(cat);
        createdCategoryIds.add(cat.getId());

        Product prod = new Product();
        prod.setCategory(cat);
        prod.setName("Prod");
        prod.setActive(true);
        testProduct = productRepository.saveAndFlush(prod);
        createdProductIds.add(testProduct.getId());
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
                    orderItemRepository.deleteAll(order.getItems());
                    order.getItems().clear();
                    customerOrderRepository.saveAndFlush(order);
                    customerOrderRepository.deleteById(orderId);
                });
            }
            for (Long addressId : createdAddressIds) {
                if (customerAddressRepository.existsById(addressId)) {
                    customerAddressRepository.deleteById(addressId);
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

        transactionTemplate.executeWithoutResult(s -> {
            for (Long deliveryId : createdDeliveryAddressIds) {
                assertTrue(orderDeliveryAddressRepository.findById(deliveryId).isEmpty(), "Delivery snapshot leak");
            }
            for (Long orderId : createdOrderIds) {
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak");
            }
        });
    }

    private CustomerAddress createAddress(User user, boolean isDefault) {
        CustomerAddress addr = new CustomerAddress();
        addr.setUser(user);
        addr.setRecipientName("Recipient " + user.getId());
        addr.setPhoneNumber("12345");
        addr.setAddressLine1("Line1 " + UUID.randomUUID());
        addr.setCity("City");
        addr.setCountry("USA");
        addr.setIsDefault(isDefault);
        addr = customerAddressRepository.saveAndFlush(addr);
        createdAddressIds.add(addr.getId());
        return addr;
    }

    private ProductVariant createSavedVariant(int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(testProduct);
        variant.setSku("SKU-" + UUID.randomUUID());
        variant.setSize("S");
        variant.setColor("Red");
        variant.setPrice(new BigDecimal("10.00"));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.saveAndFlush(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private void addCartItem(User user, ProductVariant variant, int quantity) {
        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart = cartRepository.saveAndFlush(newCart);
            createdCartIds.add(newCart.getId());
            return newCart;
        });

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.saveAndFlush(item);
        createdCartItemIds.add(item.getId());
    }

    @Test
    void defaultAddressCheckoutSucceeds() {
        CustomerAddress addr = createAddress(customer1, true);
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        OrderResponseDto response = orderService.checkout(customer1.getId(), null);
        createdOrderIds.add(response.id());

        transactionTemplate.executeWithoutResult(s -> {
            Optional<OrderDeliveryAddress> deliveryOpt = orderDeliveryAddressRepository.findByOrderId(response.id());
            assertTrue(deliveryOpt.isPresent());
            OrderDeliveryAddress delivery = deliveryOpt.get();
            createdDeliveryAddressIds.add(delivery.getId());

            assertEquals(addr.getRecipientName(), delivery.getRecipientName());
            assertEquals(addr.getAddressLine1(), delivery.getAddressLine1());
        });

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(9, updated.getStockQuantity());
    }

    @Test
    void explicitOwnedAddressCheckoutSucceeds() {
        createAddress(customer1, true);
        CustomerAddress addr2 = createAddress(customer1, false);

        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        OrderResponseDto response = orderService.checkout(customer1.getId(), addr2.getId());
        createdOrderIds.add(response.id());

        transactionTemplate.executeWithoutResult(s -> {
            Optional<OrderDeliveryAddress> deliveryOpt = orderDeliveryAddressRepository.findByOrderId(response.id());
            assertTrue(deliveryOpt.isPresent());
            OrderDeliveryAddress delivery = deliveryOpt.get();
            createdDeliveryAddressIds.add(delivery.getId());

            assertEquals(addr2.getRecipientName(), delivery.getRecipientName());
            assertEquals(addr2.getAddressLine1(), delivery.getAddressLine1());
        });
    }

    @Test
    void missingDefaultFailsWithoutMutation() {
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer1.getId(), null));
        assertEquals("Delivery address is required for checkout", ex.getMessage());

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updated.getStockQuantity());
    }

    @Test
    void foreignAddressFailsWithoutMutation() {
        CustomerAddress foreignAddr = createAddress(customer2, false);

        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer1.getId(), foreignAddr.getId()));
        assertEquals("Delivery address is required for checkout", ex.getMessage());

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updated.getStockQuantity());
    }

    @Test
    void missingExplicitAddressFailsWithoutMutation() {
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer1.getId(), 9999L));
        assertEquals("Delivery address is required for checkout", ex.getMessage());

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updated.getStockQuantity());
    }

    @Test
    void snapshotRemainsImmutableAfterAddressUpdateOrDeletion() {
        CustomerAddress addr = createAddress(customer1, true);
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        OrderResponseDto response = orderService.checkout(customer1.getId(), null);
        createdOrderIds.add(response.id());

        transactionTemplate.executeWithoutResult(s -> {
            Optional<OrderDeliveryAddress> deliveryOpt = orderDeliveryAddressRepository.findByOrderId(response.id());
            deliveryOpt.ifPresent(d -> createdDeliveryAddressIds.add(d.getId()));
        });

        // Mutate original address
        addr.setRecipientName("Updated Name");
        customerAddressRepository.saveAndFlush(addr);

        transactionTemplate.executeWithoutResult(s -> {
            OrderDeliveryAddress snapshot = orderDeliveryAddressRepository.findByOrderId(response.id()).orElseThrow();
            assertNotEquals("Updated Name", snapshot.getRecipientName());
        });

        customerAddressRepository.delete(addr);
        customerAddressRepository.flush();

        transactionTemplate.executeWithoutResult(s -> {
            OrderDeliveryAddress snapshot = orderDeliveryAddressRepository.findByOrderId(response.id()).orElseThrow();
            assertNotNull(snapshot);
        });
    }
}
