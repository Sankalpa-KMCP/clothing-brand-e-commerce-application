package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartItem;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
import com.clothingbrand.ecommerce.domain.catalog.Product;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariant;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
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
import java.util.List;

@Service
public class OrderService {

    private static final int MAX_ORDER_HISTORY_PAGE_SIZE = 50;

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;

    public OrderService(CartRepository cartRepository,
                        ProductVariantRepository productVariantRepository,
                        CustomerOrderRepository customerOrderRepository,
                        UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.userRepository = userRepository;
    }

    record CheckoutItemInput(Long cartId, Long userId, Long productId, Long variantId, Integer requestedQuantity) {}

    @Transactional
    public OrderResponseDto checkout(Long authenticatedUserId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("Authenticated user ID cannot be null");
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
        return new OrderDetailResponseDto(
                order.getId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getTotal(),
                order.getCreatedAt(),
                mapToItemResponses(order)
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
}
