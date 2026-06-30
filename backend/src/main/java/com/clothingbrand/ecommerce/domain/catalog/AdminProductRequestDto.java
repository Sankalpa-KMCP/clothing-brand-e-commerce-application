package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminProductRequestDto(
    @NotNull(message = "Category ID is required")
    Long categoryId,

    @NotBlank(message = "Product name is required")
    String name,

    String description,

    String imageUrl,

    @NotNull(message = "Active status is required")
    Boolean active
) {}
