package com.clothingbrand.ecommerce.domain.order;

public record OrderDeliveryAddressResponseDto(
        String recipientName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country
) {
}
