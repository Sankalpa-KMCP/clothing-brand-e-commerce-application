package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrderResponseDto response = orderService.checkout(userDetails.getUser().getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<OrderHistoryPageResponseDto> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OrderHistoryPageResponseDto response = orderService.getMyOrders(userDetails.getUser().getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getMyOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        OrderDetailResponseDto response = orderService.getMyOrder(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponseDto> cancelMyOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId) {
        OrderDetailResponseDto response = orderService.cancelMyOrder(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(response);
    }
}
