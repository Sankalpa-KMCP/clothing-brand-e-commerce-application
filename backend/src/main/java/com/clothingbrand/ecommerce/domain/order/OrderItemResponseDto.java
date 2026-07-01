package com.clothingbrand.ecommerce.domain.order;

import java.math.BigDecimal;

public record OrderItemResponseDto(
        String productName,
        String productImageUrl,
        String size,
        String color,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
