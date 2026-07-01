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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderHistoryServiceIntegrationTest {

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
        customer = createUser(customerRole, "history-a");
        customer2 = createUser(customerRole, "history-b");

        category = new Category();
        category.setName("History-Cat-" + UUID.randomUUID());
        category = categoryRepository.save(category);
        createdCategoryIds.add(category.getId());

        product = createProduct("History Product");
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
    void emptyHistory_returnsEmptyPageMetadataAndDoesNotMutate() {
        long orderCountBefore = customerOrderRepository.count();
        long cartCountBefore = cartRepository.count();

        OrderHistoryPageResponseDto response = orderService.getMyOrders(customer.getId(), 0, 20);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.first());
        assertTrue(response.last());
        assertEquals(orderCountBefore, customerOrderRepository.count());
        assertEquals(cartCountBefore, cartRepository.count());
    }

    @Test
    void customerOnlyList_returnsOnlyAuthenticatedCustomerOrdersWithoutInternals() {
        ProductVariant variantA = createVariant(product, "A", 9, "10.00");
        ProductVariant variantB = createVariant(product, "B", 9, "12.00");
        CustomerOrder orderA = createOrder(customer, variantA, "Snapshot A", "img-a.jpg", "S", "Black", 1, "10.00");
        CustomerOrder orderB = createOrder(customer2, variantB, "Snapshot B", "img-b.jpg", "M", "Blue", 1, "12.00");

        OrderHistoryPageResponseDto responseA = orderService.getMyOrders(customer.getId(), 0, 20);
        OrderHistoryPageResponseDto responseB = orderService.getMyOrders(customer2.getId(), 0, 20);

        assertEquals(List.of(orderA.getId()), responseA.content().stream().map(OrderSummaryResponseDto::id).toList());
        assertEquals(List.of(orderB.getId()), responseB.content().stream().map(OrderSummaryResponseDto::id).toList());
        assertEquals(Set.of("id", "status", "subtotal", "total", "createdAt"), recordFields(OrderSummaryResponseDto.class));
        assertEquals(Set.of("content", "page", "size", "totalElements", "totalPages", "first", "last"),
                recordFields(OrderHistoryPageResponseDto.class));
    }

    @Test
    void listUsesCreatedAtDescendingAndIdDescendingTieBreaker() {
        ProductVariant variant = createVariant(product, "Tie", 9, "10.00");
        List<Long> orderIds = createOrdersInOneTransaction(customer, variant, 3);

        OrderHistoryPageResponseDto response = orderService.getMyOrders(customer.getId(), 0, 20);

        List<Long> actualIds = response.content().stream().map(OrderSummaryResponseDto::id).toList();
        assertEquals(List.of(orderIds.get(2), orderIds.get(1), orderIds.get(0)), actualIds);
        assertEquals(customerOrderRepository.findById(orderIds.get(0)).orElseThrow().getCreatedAt(),
                customerOrderRepository.findById(orderIds.get(1)).orElseThrow().getCreatedAt());
    }

    @Test
    void paginationReturnsExpectedMetadataAndPageBoundaries() {
        ProductVariant variant = createVariant(product, "Page", 9, "10.00");
        List<Long> orderIds = createOrdersInOneTransaction(customer, variant, 5);

        OrderHistoryPageResponseDto page0 = orderService.getMyOrders(customer.getId(), 0, 2);
        OrderHistoryPageResponseDto page1 = orderService.getMyOrders(customer.getId(), 1, 2);
        OrderHistoryPageResponseDto page2 = orderService.getMyOrders(customer.getId(), 2, 2);

        assertEquals(2, page0.content().size());
        assertEquals(0, page0.page());
        assertEquals(2, page0.size());
        assertEquals(5, page0.totalElements());
        assertEquals(3, page0.totalPages());
        assertTrue(page0.first());
        assertFalse(page0.last());
        assertEquals(List.of(orderIds.get(4), orderIds.get(3)), page0.content().stream().map(OrderSummaryResponseDto::id).toList());

        assertEquals(List.of(orderIds.get(2), orderIds.get(1)), page1.content().stream().map(OrderSummaryResponseDto::id).toList());
        assertFalse(page1.first());
        assertFalse(page1.last());

        assertEquals(List.of(orderIds.get(0)), page2.content().stream().map(OrderSummaryResponseDto::id).toList());
        assertFalse(page2.first());
        assertTrue(page2.last());
    }

    @Test
    void invalidPaginationThrowsBadRequestConventionExceptions() {
        assertEquals("Page index must not be negative",
                assertThrows(IllegalArgumentException.class, () -> orderService.getMyOrders(customer.getId(), -1, 20)).getMessage());
        assertEquals("Page size must be between 1 and 50",
                assertThrows(IllegalArgumentException.class, () -> orderService.getMyOrders(customer.getId(), 0, 0)).getMessage());
        assertEquals("Page size must be between 1 and 50",
                assertThrows(IllegalArgumentException.class, () -> orderService.getMyOrders(customer.getId(), 0, 51)).getMessage());
    }

    @Test
    void ownDetailReturnsStoredSnapshotFieldsAndSafeItemDetails() {
        ProductVariant variant = createVariant(product, "Detail", 8, "15.00");
        CustomerOrder order = createOrder(customer, variant, "Snapshot Shirt", "snapshot.jpg", "L", "Green", 2, "15.00");

        OrderDetailResponseDto response = orderService.getMyOrder(customer.getId(), order.getId());

        assertEquals(order.getId(), response.id());
        assertEquals("PLACED", response.status());
        assertEquals(new BigDecimal("30.00"), response.subtotal());
        assertEquals(new BigDecimal("30.00"), response.total());
        assertNotNull(response.createdAt());
        assertEquals(1, response.items().size());
        assertEquals("Snapshot Shirt", response.items().get(0).productName());
        assertEquals("snapshot.jpg", response.items().get(0).productImageUrl());
        assertEquals("L", response.items().get(0).size());
        assertEquals("Green", response.items().get(0).color());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(new BigDecimal("15.00"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("30.00"), response.items().get(0).lineTotal());
        assertTrue(response.statusHistory().isEmpty());
        assertEquals(Set.of("productName", "productImageUrl", "size", "color", "quantity", "unitPrice", "lineTotal"),
                recordFields(OrderItemResponseDto.class));
        assertEquals(Set.of("id", "status", "subtotal", "total", "createdAt", "items", "statusHistory", "deliveryAddress"),
                recordFields(OrderDetailResponseDto.class));
        assertEquals(Set.of("previousStatus", "newStatus", "actorType", "createdAt"),
                recordFields(OrderStatusHistoryResponseDto.class));
    }

    @Test
    void foreignAndMissingDetailReturnSameNonRevealingNotFound() {
        ProductVariant variant = createVariant(product, "Foreign", 8, "15.00");
        CustomerOrder order = createOrder(customer, variant, "Private Snapshot", "private.jpg", "S", "Red", 1, "15.00");

        ResourceNotFoundException foreign = assertThrows(ResourceNotFoundException.class,
                () -> orderService.getMyOrder(customer2.getId(), order.getId()));
        ResourceNotFoundException missing = assertThrows(ResourceNotFoundException.class,
                () -> orderService.getMyOrder(customer2.getId(), 999999999L));

        assertEquals("Order not found", foreign.getMessage());
        assertEquals("Order not found", missing.getMessage());
    }

    @Test
    void detailUsesImmutableSnapshotAfterLiveCatalogChanges() {
        ProductVariant variant = createVariant(product, "Immutable", 8, "15.00");
        CustomerOrder order = createOrder(customer, variant, "Original Snapshot", "original.jpg", "M", "Black", 1, "15.00");

        product.setName("Changed Live Product");
        product.setImageUrl("changed.jpg");
        productRepository.saveAndFlush(product);
        variant.setSize("XL");
        variant.setColor("Orange");
        variant.setPrice(new BigDecimal("99.99"));
        variant.setStockQuantity(1);
        productVariantRepository.saveAndFlush(variant);

        OrderDetailResponseDto response = orderService.getMyOrder(customer.getId(), order.getId());

        assertEquals("Original Snapshot", response.items().get(0).productName());
        assertEquals("original.jpg", response.items().get(0).productImageUrl());
        assertEquals("M", response.items().get(0).size());
        assertEquals("Black", response.items().get(0).color());
        assertEquals(new BigDecimal("15.00"), response.items().get(0).unitPrice());
        assertTrue(response.statusHistory().isEmpty());
    }

    @Test
    void listAndDetailDoNotMutateOrderCartStockOrCatalogState() {
        ProductVariant variant = createVariant(product, "Readonly", 6, "20.00");
        Cart cart = createCartWithItem(customer, variant, 2);
        CustomerOrder order = createOrder(customer, variant, "Readonly Snapshot", "readonly.jpg", "S", "Blue", 1, "20.00");

        String productNameBefore = productRepository.findById(product.getId()).orElseThrow().getName();
        int stockBefore = productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
        long cartItemsBefore = cartItemCount(cart.getId());
        long ordersBefore = customerOrderRepository.count();

        orderService.getMyOrders(customer.getId(), 0, 20);
        orderService.getMyOrder(customer.getId(), order.getId());

        assertEquals(productNameBefore, productRepository.findById(product.getId()).orElseThrow().getName());
        assertEquals(stockBefore, productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity());
        assertEquals(cartItemsBefore, cartItemCount(cart.getId()));
        assertEquals(ordersBefore, customerOrderRepository.count());
    }

    private User createUser(Role role, String label) {
        User user = new User();
        user.setEmail(label + "-" + UUID.randomUUID() + "@test.com");
        user.setFirstName("History");
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
        newProduct.setImageUrl("https://example.test/history/" + UUID.randomUUID() + ".jpg");
        newProduct.setActive(true);
        newProduct = productRepository.save(newProduct);
        createdProductIds.add(newProduct.getId());
        return newProduct;
    }

    private ProductVariant createVariant(Product targetProduct, String label, int stock, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(targetProduct);
        variant.setSku("SKU-HISTORY-" + label + "-" + UUID.randomUUID());
        variant.setSize("SZ-" + label + "-" + shortId());
        variant.setColor("CL-" + label + "-" + shortId());
        variant.setPrice(new BigDecimal(price));
        variant.setStockQuantity(stock);
        variant = productVariantRepository.save(variant);
        createdVariantIds.add(variant.getId());
        return variant;
    }

    private CustomerOrder createOrder(User owner,
                                      ProductVariant variant,
                                      String productName,
                                      String productImageUrl,
                                      String size,
                                      String color,
                                      int quantity,
                                      String unitPrice) {
        CustomerOrder order = new CustomerOrder();
        order.setUser(owner);
        order.setStatus(OrderStatus.PLACED);
        BigDecimal price = new BigDecimal(unitPrice);
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
        order.setSubtotal(lineTotal);
        order.setTotal(lineTotal);
        order.addItem(createOrderItem(variant, productName, productImageUrl, size, color, quantity, price, lineTotal));

        order = customerOrderRepository.saveAndFlush(order);
        createdOrderIds.add(order.getId());
        order.getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
        return customerOrderRepository.findById(order.getId()).orElseThrow();
    }

    private List<Long> createOrdersInOneTransaction(User owner, ProductVariant variant, int count) {
        return transactionTemplate.execute(status -> {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                CustomerOrder order = new CustomerOrder();
                order.setUser(owner);
                order.setStatus(OrderStatus.PLACED);
                BigDecimal price = new BigDecimal("10.00").add(BigDecimal.valueOf(i));
                order.setSubtotal(price);
                order.setTotal(price);
                order.addItem(createOrderItem(
                        variant,
                        "Tie Snapshot " + i,
                        "tie-" + i + ".jpg",
                        "S",
                        "Black-" + i,
                        1,
                        price,
                        price
                ));
                order = customerOrderRepository.saveAndFlush(order);
                createdOrderIds.add(order.getId());
                order.getItems().forEach(item -> createdOrderItemIds.add(item.getId()));
                ids.add(order.getId());
            }
            return ids;
        });
    }

    private OrderItem createOrderItem(ProductVariant variant,
                                      String productName,
                                      String productImageUrl,
                                      String size,
                                      String color,
                                      int quantity,
                                      BigDecimal unitPrice,
                                      BigDecimal lineTotal) {
        OrderItem item = new OrderItem();
        item.setOriginalProductId(variant.getProduct().getId());
        item.setOriginalVariantId(variant.getId());
        item.setProductName(productName);
        item.setProductImageUrl(productImageUrl);
        item.setSku("ORDER-SNAPSHOT-SKU-" + UUID.randomUUID());
        item.setSize(size);
        item.setColor(color);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setLineTotal(lineTotal);
        return item;
    }

    private Cart createCartWithItem(User owner, ProductVariant variant, int quantity) {
        Cart cart = new Cart();
        cart.setUser(owner);
        cart = cartRepository.save(cart);
        createdCartIds.add(cart.getId());

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        item = cartItemRepository.save(item);
        createdCartItemIds.add(item.getId());
        return cart;
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
        return fields;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
