package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.config.StripeProperties;
import com.clothingbrand.ecommerce.domain.order.CustomerOrder;
import com.clothingbrand.ecommerce.domain.order.OrderService;
import com.clothingbrand.ecommerce.domain.order.StripeCheckoutResponseDto;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutCoordinator {

    private final OrderService orderService;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;
    private final com.clothingbrand.ecommerce.config.ObservabilityService observabilityService;

    public StripeCheckoutCoordinator(OrderService orderService,
                                     StripeGateway stripeGateway,
                                     StripeProperties stripeProperties,
                                     com.clothingbrand.ecommerce.config.ObservabilityService observabilityService) {
        this.orderService = orderService;
        this.stripeGateway = stripeGateway;
        this.stripeProperties = stripeProperties;
        this.observabilityService = observabilityService;
    }

    public StripeCheckoutResponseDto initiateCheckout(Long userId, Long addressId) throws Exception {
        if (!stripeGateway.isEnabled()) {
            throw new com.clothingbrand.ecommerce.exception.ResourceConflictException("Stripe payment gateway is disabled.");
        }

        try {
            // Step 1: Create or reuse local reservation
            CustomerOrder order = orderService.reservePaymentSession(userId, addressId);

            String successUrl = stripeProperties.getRedirectBaseUrl() + "/payment/success?session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = stripeProperties.getRedirectBaseUrl() + "/payment/cancel";

            Session session;
            if (order.getStripeSessionId() != null) {
                // Step 2a: Retrieve existing session from Stripe
                session = stripeGateway.getStripeClient().checkout().sessions().retrieve(order.getStripeSessionId());
                observabilityService.trackReservationReuse();
            } else {
                // Step 2b: Create session on Stripe using deterministic idempotency key
                session = stripeGateway.createCheckoutSession(
                        order,
                        successUrl,
                        cancelUrl,
                        stripeProperties.getSessionTimeoutSeconds()
                );

                // Step 3: Persist returned Stripe session reference
                orderService.saveStripeSessionId(order.getId(), session.getId());
                observabilityService.trackReservationCreate();
            }

            return new StripeCheckoutResponseDto(
                    order.getId(),
                    session.getUrl(),
                    order.getReservationExpiresAt()
            );
        } catch (Exception ex) {
            observabilityService.trackReservationFailure();
            throw ex;
        }
    }
}
