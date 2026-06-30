package com.clothingbrand.ecommerce.domain.catalog;

public record AdminProductResponseDto(
    Long id,
    Long categoryId,
    String name,
    String description,
    String imageUrl,
    Boolean active
) {
    public static AdminProductResponseDto fromEntity(Product product) {
        return new AdminProductResponseDto(
            product.getId(),
            product.getCategory() != null ? product.getCategory().getId() : null,
            product.getName(),
            product.getDescription(),
            product.getImageUrl(),
            product.getActive()
        );
    }
}
