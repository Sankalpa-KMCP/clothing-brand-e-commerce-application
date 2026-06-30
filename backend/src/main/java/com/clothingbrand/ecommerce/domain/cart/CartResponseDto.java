package com.clothingbrand.ecommerce.domain.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDto(
    List<CartItemResponseDto> items,
    BigDecimal cartTotal,
    Integer totalQuantity
) {}
