package com.clothingbrand.ecommerce.domain.order;

import jakarta.validation.constraints.Positive;

public record CheckoutRequestDto(
        @Positive(message = "Address ID must be positive")
        Long addressId
) {}
