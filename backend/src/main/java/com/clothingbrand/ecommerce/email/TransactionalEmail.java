package com.clothingbrand.ecommerce.email;

public record TransactionalEmail(
        String to,
        String subject,
        String textBody
) {}
