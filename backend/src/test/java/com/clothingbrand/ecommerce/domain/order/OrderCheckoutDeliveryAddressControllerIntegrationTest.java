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
import com.clothingbrand.ecommerce.security.JwtService;
import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class OrderCheckoutDeliveryAddressControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

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
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customer1;
    private User customer2;
    private User admin;
    private String customer1Token;
    private String customer2Token;
    private String adminToken;
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
    void defaultAddressCheckoutNoBodySucceeds() throws Exception {
        CustomerAddress addr = createAddress(customer1, true);
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andReturn();

        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        createdOrderIds.add(orderId);

        transactionTemplate.executeWithoutResult(s -> {
            Optional<OrderDeliveryAddress> deliveryOpt = orderDeliveryAddressRepository.findByOrderId(orderId);
            assertTrue(deliveryOpt.isPresent());
            OrderDeliveryAddress delivery = deliveryOpt.get();
            createdDeliveryAddressIds.add(delivery.getId());
            assertEquals(addr.getRecipientName(), delivery.getRecipientName());
        });
    }

    @Test
    void explicitAddressCheckoutSucceeds() throws Exception {
        createAddress(customer1, true);
        CustomerAddress addr2 = createAddress(customer1, false);
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        CheckoutRequestDto req = new CheckoutRequestDto(addr2.getId());

        MvcResult result = mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        createdOrderIds.add(orderId);

        transactionTemplate.executeWithoutResult(s -> {
            Optional<OrderDeliveryAddress> deliveryOpt = orderDeliveryAddressRepository.findByOrderId(orderId);
            assertTrue(deliveryOpt.isPresent());
            OrderDeliveryAddress delivery = deliveryOpt.get();
            createdDeliveryAddressIds.add(delivery.getId());
            assertEquals(addr2.getRecipientName(), delivery.getRecipientName());
        });
    }

    @Test
    void missingDefaultNoBodyReturns409() throws Exception {
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Delivery address is required for checkout"));
    }

    @Test
    void foreignExplicitAddressReturns409() throws Exception {
        CustomerAddress foreignAddr = createAddress(customer2, false);
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        CheckoutRequestDto req = new CheckoutRequestDto(foreignAddr.getId());

        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Delivery address is required for checkout"));
    }

    @Test
    void invalidAddressIdReturns400() throws Exception {
        ProductVariant variant = createSavedVariant(10);
        addCartItem(customer1, variant, 1);

        CheckoutRequestDto req = new CheckoutRequestDto(-1L);

        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/orders/checkout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminTokenReturns403() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
