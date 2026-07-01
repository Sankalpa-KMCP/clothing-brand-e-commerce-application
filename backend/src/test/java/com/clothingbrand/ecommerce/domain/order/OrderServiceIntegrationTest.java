package com.clothingbrand.ecommerce.domain.order;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderServiceIntegrationTest {

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
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private User customer;
    private User customer2;

    private Product testProduct;

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

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();

        customer = new User();
        customer.setEmail("cust1-" + UUID.randomUUID() + "@test.com");
        customer.setFirstName("Customer");
        customer.setLastName("One");
        customer.setPasswordHash("password");
        customer.setRole(customerRole);
        customer.setActive(true);
        customer = userRepository.save(customer);
        createdUserIds.add(customer.getId());

        customer2 = new User();
        customer2.setEmail("cust2-" + UUID.randomUUID() + "@test.com");
        customer2.setFirstName("Customer");
        customer2.setLastName("Two");
        customer2.setPasswordHash("password");
        customer2.setRole(customerRole);
        customer2.setActive(true);
        customer2 = userRepository.save(customer2);
        createdUserIds.add(customer2.getId());

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
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
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

            for (Long userId : createdUserIds) {
                if (userRepository.existsById(userId)) {
                    userRepository.deleteById(userId);
                }
            }
        });

        transactionTemplate.executeWithoutResult(status -> {
            for (Long orderId : createdOrderIds) {
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak: " + orderId);
            }
            for (Long cartItemId : createdCartItemIds) {
                assertTrue(cartItemRepository.findById(cartItemId).isEmpty(), "CartItem leak: " + cartItemId);
            }
            for (Long cartId : createdCartIds) {
                assertTrue(cartRepository.findById(cartId).isEmpty(), "Cart leak: " + cartId);
            }
            for (Long variantId : createdVariantIds) {
                assertTrue(productVariantRepository.findById(variantId).isEmpty(), "Variant leak: " + variantId);
            }
            for (Long productId : createdProductIds) {
                assertTrue(productRepository.findById(productId).isEmpty(), "Product leak: " + productId);
            }
            for (Long categoryId : createdCategoryIds) {
                assertTrue(categoryRepository.findById(categoryId).isEmpty(), "Category leak: " + categoryId);
            }
            for (Long userId : createdUserIds) {
                assertTrue(userRepository.findById(userId).isEmpty(), "User leak: " + userId);
            }
        });
    }

    private ProductVariant createSavedVariant(Product product, String sku, String size, String color, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
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

    private long getCartItemCount(Long cartId) {
        return createdCartItemIds.stream()
                .filter(id -> cartItemRepository.findById(id)
                        .map(ci -> ci.getCart().getId().equals(cartId))
                        .orElse(false))
                .count();
    }

    @Test
    void test1_NoPersistedCart_ThrowsConflict() {
        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> {
            orderService.checkout(customer.getId());
        });
        assertEquals("Cart is empty", ex.getMessage());
    }

    @Test
    void test2_PersistedEmptyCart_ThrowsConflict() {
        Cart newCart = new Cart();
        newCart.setUser(customer);
        newCart = cartRepository.save(newCart);
        createdCartIds.add(newCart.getId());

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> {
            orderService.checkout(customer.getId());
        });
        assertEquals("Cart is empty", ex.getMessage());
    }

    @Test
    void test3_SuccessfulSingleItemCheckout() {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-S1-" + UUID.randomUUID(), "S", "Red", 10, "15.00");
        addCartItem(customer, variant, 2);

        OrderResponseDto response = orderService.checkout(customer.getId());
        createdOrderIds.add(response.id());

        assertNotNull(response.id());
        assertEquals(OrderStatus.PLACED.name(), response.status());
        assertEquals(new BigDecimal("30.00"), response.subtotal());
        assertEquals(new BigDecimal("30.00"), response.total());
        assertEquals(1, response.items().size());

        OrderItemResponseDto itemResponse = response.items().get(0);
        assertEquals("Test Product", itemResponse.productName());
        assertEquals("S", itemResponse.size());
        assertEquals("Red", itemResponse.color());
        assertEquals(2, itemResponse.quantity());
        assertEquals(new BigDecimal("15.00"), itemResponse.unitPrice());
        assertEquals(new BigDecimal("30.00"), itemResponse.lineTotal());

        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = customerOrderRepository.findById(response.id()).orElseThrow();
            assertEquals(1, order.getItems().size());
            assertEquals(customer.getId(), order.getUser().getId());
        });

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(8, updatedVariant.getStockQuantity());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(0, getCartItemCount(cart.getId()));
    }

    @Test
    void test4_SuccessfulMultiItemCheckout() {
        ProductVariant var1 = createSavedVariant(testProduct, "SKU-M1-" + UUID.randomUUID(), "S", "Red", 5, "10.00");
        ProductVariant var2 = createSavedVariant(testProduct, "SKU-M2-" + UUID.randomUUID(), "M", "Blue", 5, "20.00");

        addCartItem(customer, var1, 1);
        addCartItem(customer, var2, 2);

        OrderResponseDto response = orderService.checkout(customer.getId());
        createdOrderIds.add(response.id());

        assertEquals(new BigDecimal("50.00"), response.total());
        assertEquals(2, response.items().size());

        ProductVariant updated1 = productVariantRepository.findById(var1.getId()).orElseThrow();
        assertEquals(4, updated1.getStockQuantity());

        ProductVariant updated2 = productVariantRepository.findById(var2.getId()).orElseThrow();
        assertEquals(3, updated2.getStockQuantity());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(0, getCartItemCount(cart.getId()));
    }

    @Test
    void test5_CheckoutTimePriceSnapshot() {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-P1-" + UUID.randomUUID(), "S", "Red", 10, "10.00");
        addCartItem(customer, variant, 1);

        variant.setPrice(new BigDecimal("12.50"));
        productVariantRepository.save(variant);

        OrderResponseDto response = orderService.checkout(customer.getId());
        createdOrderIds.add(response.id());

        assertEquals(new BigDecimal("12.50"), response.total());
        assertEquals(new BigDecimal("12.50"), response.items().get(0).unitPrice());

        transactionTemplate.executeWithoutResult(status -> {
            CustomerOrder order = customerOrderRepository.findById(response.id()).orElseThrow();
            assertEquals(new BigDecimal("12.50"), order.getItems().get(0).getUnitPrice());
        });
    }

    @Test
    void test6_InactiveProductRejection() {
        Product inactiveProduct = new Product();
        inactiveProduct.setCategory(testProduct.getCategory());
        inactiveProduct.setName("Inactive");
        inactiveProduct.setActive(false);
        inactiveProduct = productRepository.save(inactiveProduct);
        createdProductIds.add(inactiveProduct.getId());

        ProductVariant variant = createSavedVariant(inactiveProduct, "SKU-I1-" + UUID.randomUUID(), "S", "Red", 10, "10.00");
        addCartItem(customer, variant, 1);

        assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer.getId()));

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updatedVariant.getStockQuantity());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(1, getCartItemCount(cart.getId()));
    }

    @Test
    void test7_InsufficientStockRollbackAcrossMultipleLines() {
        ProductVariant var1 = createSavedVariant(testProduct, "SKU-R1-" + UUID.randomUUID(), "S", "Red", 10, "10.00");
        ProductVariant var2 = createSavedVariant(testProduct, "SKU-R2-" + UUID.randomUUID(), "M", "Blue", 1, "20.00");

        addCartItem(customer, var1, 1);
        addCartItem(customer, var2, 5);

        assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer.getId()));

        ProductVariant updated1 = productVariantRepository.findById(var1.getId()).orElseThrow();
        assertEquals(10, updated1.getStockQuantity());

        ProductVariant updated2 = productVariantRepository.findById(var2.getId()).orElseThrow();
        assertEquals(1, updated2.getStockQuantity());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(2, getCartItemCount(cart.getId()));
    }

    @Test
    void test8_CustomerIsolation() {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-C1-" + UUID.randomUUID(), "S", "Red", 10, "10.00");
        addCartItem(customer, variant, 1);

        assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer2.getId()));

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updated.getStockQuantity());

        Cart cart = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertEquals(1, getCartItemCount(cart.getId()));
    }

    @Test
    void test9_NoAccidentalStockReservation() {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-N1-" + UUID.randomUUID(), "S", "Red", 2, "10.00");
        addCartItem(customer, variant, 3);

        assertThrows(ResourceConflictException.class, () -> orderService.checkout(customer.getId()));

        ProductVariant updated = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(2, updated.getStockQuantity());
    }

    @Test
    void test10_ConfirmationDataBoundary() {
        ProductVariant variant = createSavedVariant(testProduct, "SKU-B1-" + UUID.randomUUID(), "S", "Red", 10, "15.00");
        addCartItem(customer, variant, 2);

        OrderResponseDto response = orderService.checkout(customer.getId());
        createdOrderIds.add(response.id());

        assertNotNull(response.id());
        assertNotNull(response.status());
        assertNotNull(response.subtotal());
        assertNotNull(response.total());
        assertEquals(1, response.items().size());

        OrderItemResponseDto itemResponse = response.items().get(0);
        assertNotNull(itemResponse.productName());
        assertNotNull(itemResponse.size());
        assertNotNull(itemResponse.color());
        assertNotNull(itemResponse.quantity());
        assertNotNull(itemResponse.unitPrice());
        assertNotNull(itemResponse.lineTotal());
        assertNull(itemResponse.productImageUrl()); // Currently null because product has no image url set
    }
}
