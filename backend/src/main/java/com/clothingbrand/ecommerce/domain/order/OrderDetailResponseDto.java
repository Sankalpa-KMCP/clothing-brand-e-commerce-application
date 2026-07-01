package com.clothingbrand.ecommerce.domain.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderDetailResponseDto(
        Long id,
        String status,
        BigDecimal subtotal,
        BigDecimal total,
        OffsetDateTime createdAt,
        List<OrderItemResponseDto> items
) {
}
