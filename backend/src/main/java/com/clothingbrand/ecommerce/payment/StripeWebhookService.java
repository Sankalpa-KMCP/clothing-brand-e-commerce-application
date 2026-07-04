package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.config.StripeProperties;
import com.clothingbrand.ecommerce.domain.cart.Cart;
import com.clothingbrand.ecommerce.domain.cart.CartRepository;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
import com.clothingbrand.ecommerce.domain.order.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private final StripeProperties stripeProperties;
    private final CustomerOrderRepository customerOrderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    public StripeWebhookService(StripeProperties stripeProperties,
                                CustomerOrderRepository customerOrderRepository,
                                WebhookEventRepository webhookEventRepository,
                                OrderStatusHistoryRepository orderStatusHistoryRepository,
                                CartRepository cartRepository,
                                ProductVariantRepository productVariantRepository) {
        this.stripeProperties = stripeProperties;
        this.customerOrderRepository = customerOrderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) throws SignatureVerificationException {
        if (!stripeProperties.isEnabled()) {
            throw new IllegalStateException("Stripe payment gateway is disabled.");
        }

        // 1. Verify Stripe Webhook Signature
        Event event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());

        // 2. Idempotency Check: Save event ID first. If it already exists, throw/return.
        if (webhookEventRepository.existsById(event.getId())) {
            return;
        }

        String eventType = event.getType();
        if ("checkout.session.completed".equals(eventType)) {
            Session session = deserializeSession(event);
            if (session != null) {
                String paymentStatus = session.getPaymentStatus();
                if ("paid".equals(paymentStatus)) {
                    String orderIdMeta = session.getMetadata() != null ? session.getMetadata().get("order_id") : null;
                    if (orderIdMeta == null) {
                        throw new IllegalArgumentException("Missing order_id in metadata");
                    }
                    Long orderId = Long.valueOf(orderIdMeta);
                    
                    CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                            .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

                    if (order.getStripeSessionId() == null || !order.getStripeSessionId().equals(session.getId())) {
                        throw new IllegalArgumentException("Stripe session ID mismatch");
                    }

                    webhookEventRepository.save(new WebhookEvent(event.getId(), eventType));

                    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                        order.setStatus(OrderStatus.PLACED);
                        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
                        order.setStripePaymentIntentId(session.getPaymentIntent());
                        order.setReservationExpiresAt(null);
                        customerOrderRepository.save(order);

                        OrderStatusHistory history = new OrderStatusHistory();
                        history.setOrder(order);
                        history.setPreviousStatus(OrderStatus.PENDING_PAYMENT);
                        history.setNewStatus(OrderStatus.PLACED);
                        history.setActorType(OrderActorType.SYSTEM);
                        orderStatusHistoryRepository.save(history);

                        Cart cart = cartRepository.findByUserId(order.getUser().getId()).orElse(null);
                        if (cart != null) {
                            cart.getItems().clear();
                            cartRepository.save(cart);
                        }
                    }
                }
            }
        } else if ("checkout.session.expired".equals(eventType)) {
            Session session = deserializeSession(event);
            if (session != null) {
                String orderIdMeta = session.getMetadata() != null ? session.getMetadata().get("order_id") : null;
                if (orderIdMeta == null) {
                    throw new IllegalArgumentException("Missing order_id in metadata");
                }
                Long orderId = Long.valueOf(orderIdMeta);

                CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

                if (order.getStripeSessionId() == null || !order.getStripeSessionId().equals(session.getId())) {
                    throw new IllegalArgumentException("Stripe session ID mismatch");
                }

                webhookEventRepository.save(new WebhookEvent(event.getId(), eventType));

                if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
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
        } else {
            webhookEventRepository.save(new WebhookEvent(event.getId(), eventType));
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

    private Session deserializeSession(Event event) {
        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            return (Session) event.getDataObjectDeserializer().getObject().get();
        }
        try {
            return (Session) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (Exception e) {
            return null;
        }
    }
}
