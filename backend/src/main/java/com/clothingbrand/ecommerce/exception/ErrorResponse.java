package com.clothingbrand.ecommerce.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        String correlationId
) {}
