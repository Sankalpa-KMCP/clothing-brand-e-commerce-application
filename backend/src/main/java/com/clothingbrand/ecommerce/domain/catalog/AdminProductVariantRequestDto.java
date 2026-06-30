package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AdminProductVariantRequestDto(
    @NotBlank(message = "SKU is required")
    String sku,
    
    @NotBlank(message = "Size is required")
    String size,
    
    @NotBlank(message = "Color is required")
    String color,
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be strictly positive")
    BigDecimal price
) {}
