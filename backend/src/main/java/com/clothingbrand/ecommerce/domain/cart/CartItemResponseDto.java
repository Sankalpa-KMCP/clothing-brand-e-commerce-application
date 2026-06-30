package com.clothingbrand.ecommerce.domain.cart;

import java.math.BigDecimal;

public record CartItemResponseDto(
    Long cartItemId,
    Long productId,
    String productName,
    String imageUrl,
    Long variantId,
    String size,
    String color,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    Boolean available
) {}
