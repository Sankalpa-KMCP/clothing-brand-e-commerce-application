package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
import com.clothingbrand.ecommerce.domain.order.*;
import com.clothingbrand.ecommerce.util.DateTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentReservationRecoveryService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final DateTimeProvider dateTimeProvider;

    public PaymentReservationRecoveryService(CustomerOrderRepository customerOrderRepository,
                                            ProductVariantRepository productVariantRepository,
                                            OrderStatusHistoryRepository orderStatusHistoryRepository,
                                            DateTimeProvider dateTimeProvider) {
        this.customerOrderRepository = customerOrderRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverExpiredOrder(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT 
                && order.getPaymentStatus() == PaymentStatus.PENDING 
                && order.getReservationExpiresAt() != null 
                && !order.getReservationExpiresAt().isAfter(dateTimeProvider.now())) {
            
            restoreStockFromSnapshots(order);

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setReservationExpiresAt(null);
            customerOrderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setPreviousStatus(OrderStatus.PENDING_PAYMENT);
            history.setNewStatus(OrderStatus.CANCELLED);
            history.setActorType(OrderActorType.SYSTEM);
            orderStatusHistoryRepository.save(history);
        }
    }

    @Transactional
    public void recoverExpiredOrderSynchronously(Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT 
                && order.getPaymentStatus() == PaymentStatus.PENDING 
                && order.getReservationExpiresAt() != null 
                && !order.getReservationExpiresAt().isAfter(dateTimeProvider.now())) {
            
            restoreStockFromSnapshots(order);

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setReservationExpiresAt(null);
            customerOrderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setPreviousStatus(OrderStatus.PENDING_PAYMENT);
            history.setNewStatus(OrderStatus.CANCELLED);
            history.setActorType(OrderActorType.SYSTEM);
            orderStatusHistoryRepository.save(history);
        }
    }

    private void restoreStockFromSnapshots(CustomerOrder order) {
        record RestockLine(Long productId, Long variantId, Integer quantity) {}
        java.util.List<RestockLine> restockLines = order.getItems().stream()
                .map(item -> new RestockLine(item.getOriginalProductId(), item.getOriginalVariantId(), item.getQuantity()))
                .sorted(java.util.Comparator.comparing(RestockLine::variantId))
                .toList();

        for (RestockLine line : restockLines) {
            int updatedRows = productVariantRepository.adjustStock(line.productId(), line.variantId(), line.quantity());
            if (updatedRows == 0) {
                throw new com.clothingbrand.ecommerce.exception.ResourceConflictException("Order cannot be restocked");
            }
        }
    }
}
