package com.clothingbrand.ecommerce.domain.order;

import java.time.OffsetDateTime;

public record OrderStatusHistoryResponseDto(
        String previousStatus,
        String newStatus,
        String actorType,
        OffsetDateTime createdAt
) {
}
