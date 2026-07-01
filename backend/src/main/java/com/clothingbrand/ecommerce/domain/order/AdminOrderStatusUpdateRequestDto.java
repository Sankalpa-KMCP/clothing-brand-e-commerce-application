package com.clothingbrand.ecommerce.domain.order;

import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusUpdateRequestDto(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
