package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.constraints.NotBlank;

public record AdminCategoryRequestDto(
    @NotBlank(message = "Category name is required")
    String name,

    String description,

    String imageUrl
) {}
