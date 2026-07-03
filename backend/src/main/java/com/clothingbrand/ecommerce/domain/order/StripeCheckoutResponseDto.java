package com.clothingbrand.ecommerce.domain.order;

import java.time.OffsetDateTime;

public record StripeCheckoutResponseDto(
        Long orderId,
        String stripeCheckoutUrl,
        OffsetDateTime reservationExpiresAt
) {}
