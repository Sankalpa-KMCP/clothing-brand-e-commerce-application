package com.clothingbrand.ecommerce.domain.order;

import java.util.List;

public record OrderHistoryPageResponseDto(
        List<OrderSummaryResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
