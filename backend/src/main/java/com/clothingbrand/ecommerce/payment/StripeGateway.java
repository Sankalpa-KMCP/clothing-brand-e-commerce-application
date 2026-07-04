package com.clothingbrand.ecommerce.payment;

import com.clothingbrand.ecommerce.config.StripeProperties;
import com.clothingbrand.ecommerce.domain.order.CustomerOrder;
import com.clothingbrand.ecommerce.domain.order.OrderItem;
import com.stripe.StripeClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class StripeGateway {

    private final StripeProperties properties;
    private StripeClient stripeClient;

    public StripeGateway(StripeProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            properties.validate();
            this.stripeClient = new StripeClient(properties.getSecretKey());
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public StripeClient getStripeClient() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Stripe payment gateway is disabled.");
        }
        return stripeClient;
    }

    public StripeProperties getProperties() {
        return properties;
    }

    public com.stripe.model.checkout.Session createCheckoutSession(CustomerOrder order, String successUrl, String cancelUrl, int timeoutSeconds) throws com.stripe.exception.StripeException {
        String idempotencyKey = "order-session-" + order.getId();

        java.util.List<com.stripe.param.checkout.SessionCreateParams.LineItem> lineItems = new java.util.ArrayList<>();
        for (OrderItem item : order.getItems()) {
            long unitPriceInCents = item.getUnitPrice().multiply(new java.math.BigDecimal("100")).longValue();

            com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData priceData = com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(properties.getCurrency())
                    .setUnitAmount(unitPriceInCents)
                    .setProductData(
                            com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getProductName())
                                    .build()
                    )
                    .build();

            com.stripe.param.checkout.SessionCreateParams.LineItem lineItem = com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                    .setQuantity(Long.valueOf(item.getQuantity()))
                    .setPriceData(priceData)
                    .build();

            lineItems.add(lineItem);
        }

        long expiresAtEpoch = java.time.Instant.now().getEpochSecond() + timeoutSeconds;
        long nowEpoch = java.time.Instant.now().getEpochSecond();
        if (expiresAtEpoch < nowEpoch + 1800) {
            expiresAtEpoch = nowEpoch + 1800; // minimum 30 minutes for Stripe API validation
        }

        com.stripe.param.checkout.SessionCreateParams params = com.stripe.param.checkout.SessionCreateParams.builder()
                .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setExpiresAt(expiresAtEpoch)
                .addAllLineItem(lineItems)
                .putMetadata("order_id", String.valueOf(order.getId()))
                .build();

        com.stripe.net.RequestOptions options = com.stripe.net.RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        return getStripeClient().checkout().sessions().create(params, options);
    }
}
