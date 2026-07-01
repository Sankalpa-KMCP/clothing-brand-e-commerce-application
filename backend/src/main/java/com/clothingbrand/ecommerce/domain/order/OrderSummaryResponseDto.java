package com.clothingbrand.ecommerce.domain.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryResponseDto(
        Long id,
        String status,
        BigDecimal subtotal,
        BigDecimal total,
        OffsetDateTime createdAt
) {
}
