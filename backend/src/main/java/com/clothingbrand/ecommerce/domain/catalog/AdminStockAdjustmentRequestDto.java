package com.clothingbrand.ecommerce.domain.catalog;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record AdminStockAdjustmentRequestDto(
    @NotNull(message = "Adjustment is required")
    Integer adjustment
) {
    @JsonIgnore
    @AssertTrue(message = "Adjustment cannot be zero")
    public boolean isValidAdjustment() {
        return adjustment == null || adjustment != 0;
    }
}
