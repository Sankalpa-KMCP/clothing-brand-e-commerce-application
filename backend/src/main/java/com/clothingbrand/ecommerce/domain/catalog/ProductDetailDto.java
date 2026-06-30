package com.clothingbrand.ecommerce.domain.catalog;

import java.util.List;

public record ProductDetailDto(
    Long id,
    String name,
    String description,
    String imageUrl,
    CategoryDto category,
    List<ProductVariantDto> variants
) {
    public static ProductDetailDto fromEntity(Product product) {
        CategoryDto categoryDto = null;
        if (product.getCategory() != null) {
            categoryDto = new CategoryDto(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription(),
                product.getCategory().getImageUrl()
            );
        }

        List<ProductVariantDto> variantDtos = product.getVariants().stream()
            .map(ProductVariantDto::fromEntity)
            .toList();

        return new ProductDetailDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getImageUrl(),
            categoryDto,
            variantDtos
        );
    }
}
