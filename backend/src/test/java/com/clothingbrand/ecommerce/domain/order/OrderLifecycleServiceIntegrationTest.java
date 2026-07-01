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
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
import com.clothingbrand.ecommerce.domain.order.OrderDeliveryAddressRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderLifecycleServiceIntegrationTest {

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
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private OrderDeliveryAddressRepository orderDeliveryAddressRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User customer;
    private User customer2;
    private User admin;
    private Category category;
    private Product product;

    private final List<Long> createdOrderIds = new ArrayList<>();
    private final List<Long> createdDeliveryAddressIds = new ArrayList<>();
    private final List<Long> createdAddressIds = new ArrayList<>();
    private final List<Long> createdOrderItemIds = new ArrayList<>();
    private final List<Long> createdHistoryIds = new ArrayList<>();
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
        createdOrderItemIds.clear();
        createdHistoryIds.clear();
        createdCartIds.clear();
        createdCartItemIds.clear();
        createdVariantIds.clear();
        createdProductIds.clear();
        createdCategoryIds.clear();
        createdUserIds.clear();

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        customer = createUser(customerRole, "lifecycle-a");
        customer2 = createUser(customerRole, "lifecycle-b");
        admin = createUser(adminRole, "lifecycle-admin");

        category = new Category();
        category.setName("Lifecycle-Cat-" + UUID.randomUUID());
        category = categoryRepository.save(category);
        createdCategoryIds.add(category.getId());

        product = createProduct("Lifecycle Product");
    }

    @AfterEach
    void cleanup() {
        transactionTemplate.executeWithoutResult(status -> {
            for (CustomerOrder order : customerOrderRepository.findAll()) {
                if (createdUserIds.contains(order.getUser().getId()) && !createdOrderIds.contains(order.getId())) {
                    createdOrderIds.add(order.getId());
                }
            }

                        for (Long deliveryId : createdDeliveryAddressIds) {
                if (orderDeliveryAddressRepository.existsById(deliveryId)) {
                    orderDeliveryAddressRepository.deleteById(deliveryId);
                }
            }
            for (Long orderId : createdOrderIds) {
                customerOrderRepository.findById(orderId).ifPresent(order -> {
                    orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).forEach(history -> {
                        if (!createdHistoryIds.contains(history.getId())) {
                            createdHistoryIds.add(history.getId());
                        }
                    });
                    order.getItems().forEach(item -> {
                        if (!createdOrderItemIds.contains(item.getId())) {
                            createdOrderItemIds.add(item.getId());
                        }
                    });
                });
            }

            for (Long historyId : createdHistoryIds) {
                if (orderStatusHistoryRepository.existsById(historyId)) {
                    orderStatusHistoryRepository.deleteById(historyId);
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

            for (Long orderItemId : createdOrderItemIds) {
                if (orderItemRepository.existsById(orderItemId)) {
                    orderItemRepository.deleteById(orderItemId);
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

        transactionTemplate.executeWithoutResult(status -> {
            for (Long historyId : createdHistoryIds) {
                assertTrue(orderStatusHistoryRepository.findById(historyId).isEmpty(), "OrderStatusHistory leak: " + historyId);
            }
            for (Long orderId : createdOrderIds) {
                            for (Long deliveryId : createdDeliveryAddressIds) {
                assertTrue(orderDeliveryAddressRepository.findById(deliveryId).isEmpty(), "Delivery leak: " + deliveryId);
            }
            for (Long addressId : createdAddressIds) {
                assertTrue(customerAddressRepository.findById(addressId).isEmpty(), "Address leak: " + addressId);
            }
                assertTrue(customerOrderRepository.findById(orderId).isEmpty(), "Order leak: " + orderId);
            }
            for (Long orderItemId : createdOrderItemIds) {
                assertTrue(orderItemRepository.findById(orderItemId).isEmpty(), "OrderItem leak: " + orderItemId);
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

    @Test
    void checkoutCreatesInitialCustomerPlacedHistoryAndSafeDetail() {
        ProductVariant variant = createVariant(product, "Initial", 5, "10.00");
        createCartWithItem(customer, variant, 2);

        OrderResponseDto checkout = checkoutAndTrack(customer);
        List<OrderStatusHistory> history = historyFor(checkout.id());
        OrderDetailResponseDto detail = orderService.getMyOrder(customer.getId(), checkout.id());

        assertEquals(1, history.size());
        assertEquals(null, history.get(0).getPreviousStatus());
        assertEquals(OrderStatus.PLACED, history.get(0).getNewStatus());
        assertEquals(OrderActorType.CUSTOMER, history.get(0).getActorType());
        assertEquals(customer.getId(), history.get(0).getActorUserId());
        assertEquals(1, detail.statusHistory().size());
        assertEquals(null, detail.statusHistory().get(0).previousStatus());
        assertEquals("PLACED", detail.statusHistory().get(0).newStatus());
        assertEquals("CUSTOMER", detail.statusHistory().get(0).actorType());
        assertEquals(Set.of("previousStatus", "newStatus", "actorType", "createdAt"),
                recordFields(OrderStatusHistoryResponseDto.class));
    }

    @Test
    void customerCancelsOwnPlacedOrderRestoresStockOnceAndKeepsCartUntouched() {
        ProductVariant variant = createVariant(product, "Cancel", 5, "10.00");
        Cart cart = createCartWithItem(customer, variant, 2);
        OrderResponseDto checkout = checkoutAndTrack(customer);

        OrderDetailResponseDto cancelled = orderService.cancelMyOrder(customer.getId(), checkout.id());

        assertEquals("CANCELLED", cancelled.status());
        assertEquals(5, variantStock(variant));
        assertEquals(0, cartItemCount(cart.getId()));
        assertEquals(List.of("PLACED", "CANCELLED"), historyFor(checkout.id()).stream().map(h -> h.getNewStatus().name()).toList());
        assertEquals("CANCELLED", cancelled.statusHistory().get(1).newStatus());
        assertEquals("CUSTOMER", cancelled.statusHistory().get(1).actorType());

        assertThrows(ResourceConflictException.class, () -> orderService.cancelMyOrder(customer.getId(), checkout.id()));
        assertEquals(5, variantStock(variant));
        assertEquals(2, historyFor(checkout.id()).size());
    }

    @Test
    void customerCannotCancelForeignProcessingShippedDeliveredOrCancelledOrders() {
        ProductVariant variant = createVariant(product, "InvalidCancel", 9, "10.00");
        createCartWithItem(customer, variant, 1);
        OrderResponseDto order = checkoutAndTrack(customer);

        assertEquals("Order not found",
                assertThrows(ResourceNotFoundException.class, () -> orderService.cancelMyOrder(customer2.getId(), order.id())).getMessage());

        orderService.updateOrderStatus(admin.getId(), order.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.PROCESSING));
        assertThrows(ResourceConflictException.class, () -> orderService.cancelMyOrder(customer.getId(), order.id()));
        orderService.updateOrderStatus(admin.getId(), order.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.SHIPPED));
        assertThrows(ResourceConflictException.class, () -> orderService.cancelMyOrder(customer.getId(), order.id()));
        orderService.updateOrderStatus(admin.getId(), order.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.DELIVERED));
        assertThrows(ResourceConflictException.class, () -> orderService.cancelMyOrder(customer.getId(), order.id()));
        assertEquals(8, variantStock(variant));
    }

    @Test
    void adminProgressionUpdatesStatusTimestampAndHistoryWithoutRestock() {
        ProductVariant variant = createVariant(product, "Progress", 4, "10.00");
        createCartWithItem(customer, variant, 1);
        OrderResponseDto checkout = checkoutAndTrack(customer);
        OffsetDateTime updatedBefore = customerOrderRepository.findById(checkout.id()).orElseThrow().getUpdatedAt();

        OrderDetailResponseDto processing = orderService.updateOrderStatus(admin.getId(), checkout.id(),
                new AdminOrderStatusUpdateRequestDto(OrderStatus.PROCESSING));
        OrderDetailResponseDto shipped = orderService.updateOrderStatus(admin.getId(), checkout.id(),
                new AdminOrderStatusUpdateRequestDto(OrderStatus.SHIPPED));
        OrderDetailResponseDto delivered = orderService.updateOrderStatus(admin.getId(), checkout.id(),
                new AdminOrderStatusUpdateRequestDto(OrderStatus.DELIVERED));
        OffsetDateTime updatedAfter = customerOrderRepository.findById(checkout.id()).orElseThrow().getUpdatedAt();

        assertEquals("PROCESSING", processing.status());
        assertEquals("SHIPPED", shipped.status());
        assertEquals("DELIVERED", delivered.status());
        assertNotEquals(updatedBefore, updatedAfter);
        assertEquals(3, variantStock(variant));
        assertEquals(List.of("PLACED", "PROCESSING", "SHIPPED", "DELIVERED"),
                historyFor(checkout.id()).stream().map(h -> h.getNewStatus().name()).toList());
    }

    @Test
    void adminCancelsPlacedAndProcessingOrdersWithAtomicRestock() {
        ProductVariant placedVariant = createVariant(product, "AdminCancelPlaced", 3, "10.00");
        createCartWithItem(customer, placedVariant, 1);
        OrderResponseDto placed = checkoutAndTrack(customer);

        OrderDetailResponseDto cancelledPlaced = orderService.updateOrderStatus(admin.getId(), placed.id(),
                new AdminOrderStatusUpdateRequestDto(OrderStatus.CANCELLED));
        assertEquals("CANCELLED", cancelledPlaced.status());
        assertEquals(3, variantStock(placedVariant));

        ProductVariant processingVariant = createVariant(product, "AdminCancelProcessing", 4, "12.00");
        createCartWithItem(customer2, processingVariant, 2);
        OrderResponseDto processing = checkoutAndTrack(customer2);
        orderService.updateOrderStatus(admin.getId(), processing.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.PROCESSING));
        orderService.updateOrderStatus(admin.getId(), processing.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.CANCELLED));

        assertEquals(4, variantStock(processingVariant));
        assertEquals(List.of("PLACED", "PROCESSING", "CANCELLED"),
                historyFor(processing.id()).stream().map(h -> h.getNewStatus().name()).toList());
    }

    @Test
    void invalidAdminTransitionsDoNotMutateStatusStockOrHistory() {
        ProductVariant variant = createVariant(product, "InvalidAdmin", 5, "10.00");
        createCartWithItem(customer, variant, 1);
        OrderResponseDto checkout = checkoutAndTrack(customer);

        assertThrows(ResourceConflictException.class,
                () -> orderService.updateOrderStatus(admin.getId(), checkout.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.PLACED)));
        assertThrows(ResourceConflictException.class,
                () -> orderService.updateOrderStatus(admin.getId(), checkout.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.SHIPPED)));

        assertEquals(OrderStatus.PLACED, customerOrderRepository.findById(checkout.id()).orElseThrow().getStatus());
        assertEquals(4, variantStock(variant));
        assertEquals(1, historyFor(checkout.id()).size());
    }

    @Test
    void restockRollbackLeavesEarlierRestockStatusAndHistoryUnchanged() {
        ProductVariant variantA = createVariant(product, "RollbackA", 5, "10.00");
        ProductVariant variantB = createVariant(product, "RollbackB", 5, "10.00");
        createCartWithItem(customer, variantA, 2);
        addCartItem(customer, variantB, 2);
        OrderResponseDto checkout = checkoutAndTrack(customer);
        int stockABefore = variantStock(variantA);

        createdVariantIds.remove(variantB.getId());
        productVariantRepository.deleteById(variantB.getId());

        assertThrows(ResourceConflictException.class, () -> orderService.cancelMyOrder(customer.getId(), checkout.id()));

        assertEquals(stockABefore, variantStock(variantA));
        assertEquals(OrderStatus.PLACED, customerOrderRepository.findById(checkout.id()).orElseThrow().getStatus());
        assertEquals(1, historyFor(checkout.id()).size());
    }

    @Test
    void adminListingFiltersPaginatesAndReadOnlyDetailsUseSnapshots() {
        ProductVariant placedVariant = createVariant(product, "ListPlaced", 5, "10.00");
        createCartWithItem(customer, placedVariant, 1);
        OrderResponseDto placed = checkoutAndTrack(customer);

        ProductVariant cancelledVariant = createVariant(product, "ListCancelled", 5, "12.00");
        createCartWithItem(customer2, cancelledVariant, 1);
        OrderResponseDto cancelled = checkoutAndTrack(customer2);
        orderService.updateOrderStatus(admin.getId(), cancelled.id(), new AdminOrderStatusUpdateRequestDto(OrderStatus.CANCELLED));

        OrderHistoryPageResponseDto all = orderService.getAdminOrders(0, 20, null);
        OrderHistoryPageResponseDto cancelledOnly = orderService.getAdminOrders(0, 20, OrderStatus.CANCELLED);
        OrderDetailResponseDto detail = orderService.getAdminOrder(placed.id());

        assertTrue(all.content().stream().map(OrderSummaryResponseDto::id).toList().containsAll(List.of(placed.id(), cancelled.id())));
        assertTrue(cancelledOnly.content().stream().map(OrderSummaryResponseDto::id).toList().contains(cancelled.id()));
        assertFalse(cancelledOnly.content().stream().map(OrderSummaryResponseDto::id).toList().contains(placed.id()));
        assertEquals("PLACED", detail.status());
        assertEquals(1, detail.statusHistory().size());
        assertEquals(Set.of("id", "status", "subtotal", "total", "createdAt", "items", "statusHistory", "deliveryAddress"),
                recordFields(OrderDetailResponseDto.class));
        assertThrows(IllegalArgumentException.class, () -> orderService.getAdminOrders(-1, 20, null));
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("Lifecycle");
        user.setLastName(label);
        user.setPasswordHash("password");
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);
        createdUserIds.add(user.getId());

        CustomerAddress address = new CustomerAddress();
        address.setUser(user);
        address.setRecipientName(user.getFirstName());
        address.setPhoneNumber("12345");
        address.setAddressLine1("Line1");
        address.setCity("City");
        address.setCountry("USA");
        address.setIsDefault(true);
        address = customerAddressRepository.save(address);
        createdAddressIds.add(address.getId());
        return user;
    }

    private Product createProduct(String label) {
        Product newProduct = new Product();
        newProduct.setCategory(category);
        newProduct.setName(label + " " + UUID.randomUUID());
        newProduct.setImageUrl("https://example.test/lifecycle/" + UUID.randomUUID() + ".jpg");
        newProduct.setActive(true);
        newProduct = productRepository.save(newProduct);
        createdProductIds.add(newProduct.getId());
        return newProduct;
    }

    private ProductVariant createVariant(Product targetProduct, String label, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(targetProduct);
        variant.setSku("SKU-LIFECYCLE-" + label + "-" + UUID.randomUUID());
        variant.setSize("SZ-" + label + "-" + shortId());
        variant.setColor("CL-" + label + "-" + shortId());
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private Cart createCartWithItem(User owner, ProductVariant variant, int quantity) {
        Cart cart = new Cart();
        cart.setUser(owner);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());
        addCartItem(owner, variant, quantity);
        return cart;
    }

    private void addCartItem(User owner, ProductVariant variant, int quantity) {
        Cart cart = cartRepository.findByUserId(owner.getId()).orElseThrow();
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
    }

    private OrderResponseDto checkoutAndTrack(User owner) {
        OrderResponseDto response = orderService.checkout(owner.getId(), null);
        createdOrderIds.add(response.id());
        transactionTemplate.executeWithoutResult(status -> {
            orderDeliveryAddressRepository.findByOrderId(response.id())
                    .ifPresent(delivery -> createdDeliveryAddressIds.add(delivery.getId()));
        });
        customerOrderRepository.findByIdAndUserIdWithItems(response.id(), owner.getId()).orElseThrow()
                .getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
        historyFor(response.id()).forEach(history -> createdHistoryIds.add(history.getId()));
        return response;
    }

    private List<OrderStatusHistory> historyFor(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId);
    }

    private int variantStock(ProductVariant variant) {
        return productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
    }

    private long cartItemCount(Long cartId) {
        return cartItemRepository.findAll().stream()
                .filter(item -> item.getCart().getId().equals(cartId))
                .count();
    }

    private Set<String> recordFields(Class<?> recordClass) {
        Set<String> fields = Arrays.stream(recordClass.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(fields.contains("sku"));
        assertFalse(fields.contains("stockQuantity"));
        assertFalse(fields.contains("userId"));
        assertFalse(fields.contains("cartId"));
        assertFalse(fields.contains("originalProductId"));
        assertFalse(fields.contains("originalVariantId"));
        assertFalse(fields.contains("actorUserId"));
        return fields;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
