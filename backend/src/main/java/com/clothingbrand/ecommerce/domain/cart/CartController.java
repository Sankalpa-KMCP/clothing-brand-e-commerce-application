package com.clothingbrand.ecommerce.domain.cart;

import com.clothingbrand.ecommerce.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponseDto> getCurrentCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponseDto response = cartService.getCurrentCart(userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CartItemRequestDto request) {
        CartResponseDto response = cartService.addCartItem(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> updateCartItemQuantity(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequestDto request) {
        CartResponseDto response = cartService.updateCartItemQuantity(userDetails.getUser().getId(), cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long cartItemId) {
        cartService.removeCartItem(userDetails.getUser().getId(), cartItemId);
        return ResponseEntity.noContent().build();
    }
}
