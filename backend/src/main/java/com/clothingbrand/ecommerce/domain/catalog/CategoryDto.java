package com.clothingbrand.ecommerce.domain.catalog;

public record CategoryDto(
    Long id,
    String name,
    String description,
    String imageUrl
) {}
