package com.clothingbrand.ecommerce.domain.catalog;

import java.math.BigDecimal;

public record AdminProductVariantResponseDto(
    Long id,
    Long productId,
    String sku,
    String size,
    String color,
    BigDecimal price,
    Integer stockQuantity
) {
    public static AdminProductVariantResponseDto fromEntity(ProductVariant variant) {
        return new AdminProductVariantResponseDto(
            variant.getId(),
            variant.getProduct().getId(),
            variant.getSku(),
            variant.getSize(),
            variant.getColor(),
            variant.getPrice(),
            variant.getStockQuantity()
        );
    }
}
