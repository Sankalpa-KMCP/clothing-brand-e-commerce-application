package com.clothingbrand.ecommerce.domain.order;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
