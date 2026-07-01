package com.clothingbrand.ecommerce.domain.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String status,
        BigDecimal subtotal,
        BigDecimal total,
        List<OrderItemResponseDto> items
) {
}
