package com.clothingbrand.ecommerce.domain.address;

public record CustomerAddressResponseDto(
        Long id,
        String label,
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        Boolean isDefault
) {}
