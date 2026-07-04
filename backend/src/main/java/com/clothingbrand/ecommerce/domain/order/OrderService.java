package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartItem;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
import com.clothingbrand.ecommerce.domain.catalog.Product;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariant;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.domain.address.CustomerAddress;
import com.clothingbrand.ecommerce.domain.address.CustomerAddressRepository;
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final int MAX_ORDER_HISTORY_PAGE_SIZE = 50;
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ADMIN_TRANSITIONS.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        ADMIN_TRANSITIONS.put(OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        ADMIN_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        ADMIN_TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ADMIN_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final OrderDeliveryAddressRepository orderDeliveryAddressRepository;
    private final com.clothingbrand.ecommerce.config.StripeProperties stripeProperties;
    private final com.clothingbrand.ecommerce.util.DateTimeProvider dateTimeProvider;
    private final com.clothingbrand.ecommerce.payment.PaymentReservationRecoveryService paymentReservationRecoveryService;

    public OrderService(CartRepository cartRepository,
                        ProductVariantRepository productVariantRepository,
                        CustomerOrderRepository customerOrderRepository,
                        OrderStatusHistoryRepository orderStatusHistoryRepository,
                        UserRepository userRepository,
                        CustomerAddressRepository customerAddressRepository,
                        OrderDeliveryAddressRepository orderDeliveryAddressRepository,
                        com.clothingbrand.ecommerce.config.StripeProperties stripeProperties,
                        com.clothingbrand.ecommerce.util.DateTimeProvider dateTimeProvider,
                        com.clothingbrand.ecommerce.payment.PaymentReservationRecoveryService paymentReservationRecoveryService) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.orderDeliveryAddressRepository = orderDeliveryAddressRepository;
        this.stripeProperties = stripeProperties;
        this.dateTimeProvider = dateTimeProvider;
        this.paymentReservationRecoveryService = paymentReservationRecoveryService;
    }

    record CheckoutItemInput(Long cartId, Long userId, Long productId, Long variantId, Integer requestedQuantity) {}
    record RestockLine(Long productId, Long variantId, Integer quantity) {}

    @Transactional
    public OrderResponseDto checkout(Long authenticatedUserId, Long addressId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }

        CustomerAddress address;
        if (addressId == null) {
            address = customerAddressRepository.findByUserIdAndIsDefaultTrue(authenticatedUserId)
                    .orElseThrow(() -> new ResourceConflictException("Delivery address is required for checkout"));
        } else {
            address = customerAddressRepository.findByIdAndUserId(addressId, authenticatedUserId)
                    .orElseThrow(() -> new ResourceConflictException("Delivery address is required for checkout"));
        }

        Cart cart = cartRepository.findByUserIdForUpdate(authenticatedUserId)
                .orElseThrow(() -> new ResourceConflictException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResourceConflictException("Cart is empty");
        }

        List<CheckoutItemInput> checkoutInputs = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
            if (item.getProductVariant() == null || item.getProductVariant().getProduct() == null) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }

            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();

            if (!product.getActive()) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
            checkoutInputs.add(new CheckoutItemInput(
                    cart.getId(),
                    authenticatedUserId,
                    product.getId(),
                    variant.getId(),
                    item.getQuantity()
            ));
        }

        checkoutInputs.sort(Comparator.comparing(CheckoutItemInput::variantId));

        for (CheckoutItemInput input : checkoutInputs) {
            int updatedRows = productVariantRepository.adjustStock(
                    input.productId(), input.variantId(), -input.requestedQuantity());
            if (updatedRows == 0) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
        }

        // Context is cleared by adjustStock. Do not use original managed entities.
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutItemInput input : checkoutInputs) {
            ProductVariant currentVariant = productVariantRepository.findById(input.variantId())
                    .orElseThrow(() -> new ResourceConflictException("Cart contains unavailable items"));
            Product currentProduct = currentVariant.getProduct();

            if (!currentProduct.getId().equals(input.productId()) || !currentProduct.getActive()) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOriginalProductId(currentProduct.getId());
            orderItem.setOriginalVariantId(currentVariant.getId());
            orderItem.setProductName(currentProduct.getName());
            orderItem.setProductImageUrl(currentProduct.getImageUrl());
            orderItem.setSku(currentVariant.getSku());
            orderItem.setSize(currentVariant.getSize());
            orderItem.setColor(currentVariant.getColor());
            orderItem.setUnitPrice(currentVariant.getPrice());
            orderItem.setQuantity(input.requestedQuantity());

            BigDecimal lineTotal = currentVariant.getPrice().multiply(BigDecimal.valueOf(input.requestedQuantity()));
            orderItem.setLineTotal(lineTotal);

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setSubtotal(subtotal);
        order.setTotal(subtotal);

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        order = customerOrderRepository.save(order);
        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountry()
        );
        orderDeliveryAddressRepository.save(deliveryAddress);

        createStatusHistory(order, null, OrderStatus.PLACED, OrderActorType.CUSTOMER, authenticatedUserId);

        // Reload cart to clear items
        Cart cartToClear = cartRepository.findById(checkoutInputs.get(0).cartId())
                .orElseThrow(() -> new ResourceConflictException("Cart not found"));
        cartToClear.getItems().clear();

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderHistoryPageResponseDto getMyOrders(Long authenticatedUserId, int page, int size) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }
        validateOrderHistoryPagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<CustomerOrder> orders = customerOrderRepository.findByUserId(authenticatedUserId, pageRequest);
        List<OrderSummaryResponseDto> content = orders.getContent().stream()
                .map(this::mapToSummaryResponse)
                .toList();

        return new OrderHistoryPageResponseDto(
                content,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getMyOrder(Long authenticatedUserId, Long orderId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        CustomerOrder order = customerOrderRepository.findByIdAndUserIdWithItems(orderId, authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return mapToDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponseDto cancelMyOrder(Long authenticatedUserId, Long orderId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        CustomerOrder lockedOrder = customerOrderRepository.findByIdAndUserIdForUpdate(orderId, authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (lockedOrder.getStatus() != OrderStatus.PLACED) {
            throw new ResourceConflictException("Order cannot be cancelled from status " + lockedOrder.getStatus().name());
        }

        restoreStockFromSnapshots(lockedOrder);
        CustomerOrder orderToUpdate = customerOrderRepository.findByIdAndUserIdForUpdate(orderId, authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        orderToUpdate.setStatus(OrderStatus.CANCELLED);
        createStatusHistory(orderToUpdate, OrderStatus.PLACED, OrderStatus.CANCELLED, OrderActorType.CUSTOMER, authenticatedUserId);

        CustomerOrder detail = customerOrderRepository.findByIdAndUserIdWithItems(orderId, authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToDetailResponse(detail);
    }

    @Transactional(readOnly = true)
    public OrderHistoryPageResponseDto getAdminOrders(int page, int size, OrderStatus status) {
        validateOrderHistoryPagination(page, size);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<CustomerOrder> orders = status == null
                ? customerOrderRepository.findAll(pageRequest)
                : customerOrderRepository.findByStatus(status, pageRequest);
        List<OrderSummaryResponseDto> content = orders.getContent().stream()
                .map(this::mapToSummaryResponse)
                .toList();

        return new OrderHistoryPageResponseDto(
                content,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getAdminOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        CustomerOrder order = customerOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponseDto updateOrderStatus(Long authenticatedAdminUserId, Long orderId, AdminOrderStatusUpdateRequestDto request) {
        if (authenticatedAdminUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        CustomerOrder lockedOrder = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus previousStatus = lockedOrder.getStatus();
        OrderStatus newStatus = request.status();
        validateAdminTransition(previousStatus, newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStockFromSnapshots(lockedOrder);
            lockedOrder = customerOrderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        }

        lockedOrder.setStatus(newStatus);
        createStatusHistory(lockedOrder, previousStatus, newStatus, OrderActorType.ADMIN, authenticatedAdminUserId);

        CustomerOrder detail = customerOrderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToDetailResponse(detail);
    }

    private OrderResponseDto mapToResponse(CustomerOrder order) {
        List<OrderItemResponseDto> itemDtos = mapToItemResponses(order);

        return new OrderResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTotal(),
                itemDtos
        );
    }

    private OrderSummaryResponseDto mapToSummaryResponse(CustomerOrder order) {
        return new OrderSummaryResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTotal(),
                order.getCreatedAt()
        );
    }

    private OrderDetailResponseDto mapToDetailResponse(CustomerOrder order) {
        OrderDeliveryAddressResponseDto deliveryDto = orderDeliveryAddressRepository.findByOrderId(order.getId())
                .map(delivery -> new OrderDeliveryAddressResponseDto(
                        delivery.getRecipientName(),
                        delivery.getPhoneNumber(),
                        delivery.getAddressLine1(),
                        delivery.getAddressLine2(),
                        delivery.getCity(),
                        delivery.getRegion(),
                        delivery.getPostalCode(),
                        delivery.getCountry()
                ))
                .orElse(null);

        return new OrderDetailResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTotal(),
                order.getCreatedAt(),
                mapToItemResponses(order),
                mapToHistoryResponses(order.getId()),
                deliveryDto
        );
    }

    private List<OrderItemResponseDto> mapToItemResponses(CustomerOrder order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getProductName(),
                        item.getProductImageUrl(),
                        item.getSize(),
                        item.getColor(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return itemDtos;
    }

    private void validateOrderHistoryPagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size <= 0 || size > MAX_ORDER_HISTORY_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and 50");
        }
    }

    private void validateAdminTransition(OrderStatus previousStatus, OrderStatus newStatus) {
        if (newStatus == previousStatus) {
            throw new ResourceConflictException("Order is already in status " + newStatus.name());
        }
        if (!ADMIN_TRANSITIONS.getOrDefault(previousStatus, Set.of()).contains(newStatus)) {
            throw new ResourceConflictException("Cannot transition order from " + previousStatus.name() + " to " + newStatus.name());
        }
    }

    private void restoreStockFromSnapshots(CustomerOrder order) {
        List<RestockLine> restockLines = order.getItems().stream()
                .map(item -> new RestockLine(item.getOriginalProductId(), item.getOriginalVariantId(), item.getQuantity()))
                .sorted(Comparator.comparing(RestockLine::variantId))
                .toList();

        for (RestockLine line : restockLines) {
            int updatedRows = productVariantRepository.adjustStock(line.productId(), line.variantId(), line.quantity());
            if (updatedRows == 0) {
                throw new ResourceConflictException("Order cannot be restocked");
            }
        }
    }

    private void createStatusHistory(CustomerOrder order,
                                     OrderStatus previousStatus,
                                     OrderStatus newStatus,
                                     OrderActorType actorType,
                                     Long actorUserId) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setActorType(actorType);
        history.setActorUserId(actorUserId);
        orderStatusHistoryRepository.save(history);
    }

    private List<OrderStatusHistoryResponseDto> mapToHistoryResponses(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).stream()
                .map(history -> new OrderStatusHistoryResponseDto(
                        history.getPreviousStatus() == null ? null : history.getPreviousStatus().name(),
                        history.getNewStatus().name(),
                        history.getActorType().name(),
                        history.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public CustomerOrder reservePaymentSession(Long authenticatedUserId, Long addressId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
        }

        java.time.OffsetDateTime now = dateTimeProvider.now();
        java.util.Optional<CustomerOrder> expiredPending = customerOrderRepository
                .findExpiredPendingPaymentOrderForUpdate(authenticatedUserId, now);
        if (expiredPending.isPresent()) {
            paymentReservationRecoveryService.recoverExpiredOrderSynchronously(expiredPending.get().getId());
        }

        java.util.Optional<CustomerOrder> existingPending = customerOrderRepository
                .findActivePendingPaymentOrderForUpdate(authenticatedUserId, now);
        if (existingPending.isPresent()) {
            return existingPending.get();
        }

        Cart cart = cartRepository.findByUserIdForUpdate(authenticatedUserId)
                .orElseThrow(() -> new ResourceConflictException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResourceConflictException("Cart is empty");
        }

        CustomerAddress address;
        if (addressId == null) {
            address = customerAddressRepository.findByUserIdAndIsDefaultTrue(authenticatedUserId)
                    .orElseThrow(() -> new ResourceConflictException("Delivery address is required for checkout"));
        } else {
            address = customerAddressRepository.findByIdAndUserId(addressId, authenticatedUserId)
                    .orElseThrow(() -> new ResourceConflictException("Delivery address is required for checkout"));
        }

        List<CheckoutItemInput> checkoutInputs = new java.util.ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
            if (item.getProductVariant() == null || item.getProductVariant().getProduct() == null) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }

            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();

            if (!product.getActive()) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
            checkoutInputs.add(new CheckoutItemInput(
                    cart.getId(),
                    authenticatedUserId,
                    product.getId(),
                    variant.getId(),
                    item.getQuantity()
            ));
        }

        checkoutInputs.sort(java.util.Comparator.comparing(CheckoutItemInput::variantId));

        for (CheckoutItemInput input : checkoutInputs) {
            int updatedRows = productVariantRepository.adjustStock(
                    input.productId(), input.variantId(), -input.requestedQuantity());
            if (updatedRows == 0) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }
        }

        List<OrderItem> orderItems = new java.util.ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutItemInput input : checkoutInputs) {
            ProductVariant currentVariant = productVariantRepository.findById(input.variantId())
                    .orElseThrow(() -> new ResourceConflictException("Cart contains unavailable items"));
            Product currentProduct = currentVariant.getProduct();

            if (!currentProduct.getId().equals(input.productId()) || !currentProduct.getActive()) {
                throw new ResourceConflictException("Cart contains unavailable items");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOriginalProductId(currentProduct.getId());
            orderItem.setOriginalVariantId(currentVariant.getId());
            orderItem.setProductName(currentProduct.getName());
            orderItem.setProductImageUrl(currentProduct.getImageUrl());
            orderItem.setSku(currentVariant.getSku());
            orderItem.setSize(currentVariant.getSize());
            orderItem.setColor(currentVariant.getColor());
            orderItem.setUnitPrice(currentVariant.getPrice());
            orderItem.setQuantity(input.requestedQuantity());

            BigDecimal lineTotal = currentVariant.getPrice().multiply(BigDecimal.valueOf(input.requestedQuantity()));
            orderItem.setLineTotal(lineTotal);

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setTotal(subtotal);

        int timeoutSeconds = stripeProperties.getSessionTimeoutSeconds();
        order.setReservationExpiresAt(now.plusSeconds(timeoutSeconds));

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        order = customerOrderRepository.save(order);
        OrderDeliveryAddress deliveryAddress = new OrderDeliveryAddress(
                order,
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountry()
        );
        orderDeliveryAddressRepository.save(deliveryAddress);

        createStatusHistory(order, null, OrderStatus.PENDING_PAYMENT, OrderActorType.CUSTOMER, authenticatedUserId);

        return order;
    }

    @Transactional
    public void saveStripeSessionId(Long orderId, String sessionId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.setStripeSessionId(sessionId);
        customerOrderRepository.save(order);
    }
}
