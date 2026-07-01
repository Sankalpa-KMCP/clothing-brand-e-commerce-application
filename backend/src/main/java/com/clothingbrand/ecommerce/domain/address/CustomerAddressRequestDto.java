package com.clothingbrand.ecommerce.domain.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerAddressRequestDto(
        @Size(max = 100, message = "Label cannot exceed 100 characters")
        String label,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 200, message = "Recipient name cannot exceed 200 characters")
        String recipientName,

        @NotBlank(message = "Phone number is required")
        @Size(max = 32, message = "Phone number cannot exceed 32 characters")
        String phoneNumber,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 120, message = "City cannot exceed 120 characters")
        String city,

        @Size(max = 120, message = "Region cannot exceed 120 characters")
        String region,

        @Size(max = 32, message = "Postal code cannot exceed 32 characters")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 120, message = "Country cannot exceed 120 characters")
        String country
) {}
