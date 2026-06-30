package com.clothingbrand.ecommerce.domain.catalog;

public record AdminCategoryResponseDto(
    Long id,
    String name,
    String description,
    String imageUrl
) {
    public static AdminCategoryResponseDto fromEntity(Category category) {
        return new AdminCategoryResponseDto(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getImageUrl()
        );
    }
}
