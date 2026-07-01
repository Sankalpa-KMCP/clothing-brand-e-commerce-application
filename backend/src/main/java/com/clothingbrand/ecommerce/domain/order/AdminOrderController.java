package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<OrderHistoryPageResponseDto> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAdminOrders(page, size, status));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getAdminOrder(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailResponseDto> updateStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId,
            @Valid @RequestBody AdminOrderStatusUpdateRequestDto request) {
        OrderDetailResponseDto response = orderService.updateOrderStatus(userDetails.getUser().getId(), orderId, request);
        return ResponseEntity.ok(response);
    }
}
