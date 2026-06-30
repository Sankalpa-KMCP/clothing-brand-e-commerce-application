package com.clothingbrand.ecommerce.domain.catalog;

import java.math.BigDecimal;

public record ProductVariantDto(
    Long id,
    String size,
    String color,
    BigDecimal price,
    boolean inStock
) {
    public static ProductVariantDto fromEntity(ProductVariant variant) {
        return new ProductVariantDto(
            variant.getId(),
            variant.getSize(),
            variant.getColor(),
            variant.getPrice(),
            variant.getStockQuantity() != null && variant.getStockQuantity() > 0
        );
    }
}
