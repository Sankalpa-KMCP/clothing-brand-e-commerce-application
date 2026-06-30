package com.clothingbrand.ecommerce.domain.catalog;

import java.math.BigDecimal;

public record ProductListDto(
    Long id,
    String name,
    String categoryName,
    String imageUrl,
    BigDecimal startingPrice
) {
    public static ProductListDto fromEntity(Product product) {
        BigDecimal lowestPrice = product.getVariants().stream()
            .map(ProductVariant::getPrice)
            .min(BigDecimal::compareTo)
            .orElse(null);

        return new ProductListDto(
            product.getId(),
            product.getName(),
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getImageUrl(),
            lowestPrice
        );
    }
}
