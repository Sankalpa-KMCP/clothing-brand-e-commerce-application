package com.clothingbrand.ecommerce.domain.cart;

import com.clothingbrand.ecommerce.domain.catalog.ProductVariant;
import com.clothingbrand.ecommerce.domain.catalog.ProductVariantRepository;
import com.clothingbrand.ecommerce.domain.user.User;
import com.clothingbrand.ecommerce.domain.user.UserRepository;
import com.clothingbrand.ecommerce.exception.ResourceConflictException;
import com.clothingbrand.ecommerce.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository productVariantRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    public CartResponseDto getCurrentCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(this::buildCartResponse)
                .orElseGet(this::emptyCartResponse);
    }

    @Transactional
    public CartResponseDto addCartItem(Long userId, CartItemRequestDto request) {
        if (request == null || request.variantId() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Valid request, variant ID, and positive quantity are required");
        }

        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found with id: " + request.variantId()));

        if (Boolean.FALSE.equals(variant.getProduct().getActive())) {
            throw new ResourceConflictException("Cannot add an inactive product to the cart");
        }

        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        CartItem existingItem = null;

        if (cart != null) {
            existingItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId()).orElse(null);
        }

        long currentQuantity = existingItem != null ? existingItem.getQuantity() : 0L;
        long newQuantityLong = currentQuantity + request.quantity();

        if (newQuantityLong > Integer.MAX_VALUE) {
            throw new ResourceConflictException("Requested quantity exceeds maximum allowed limits");
        }
        
        int newQuantity = (int) newQuantityLong;

        if (newQuantity > variant.getStockQuantity()) {
            throw new ResourceConflictException("Insufficient stock for this variant");
        }

        if (cart == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductVariant(variant);
            newItem.setQuantity(newQuantity);
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Force a flush to ensure we read the latest state when building the response
        cartRepository.flush();

        return buildCartResponse(cartRepository.findByUserId(userId).orElseThrow());
    }

    @Transactional
    public CartResponseDto updateCartItemQuantity(Long userId, Long cartItemId, CartItemUpdateRequestDto request) {
        if (request == null || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Valid request and positive quantity are required");
        }

        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        ProductVariant variant = cartItem.getProductVariant();

        if (Boolean.FALSE.equals(variant.getProduct().getActive())) {
            throw new ResourceConflictException("Cannot update quantity for an inactive product");
        }

        if (request.quantity() > variant.getStockQuantity()) {
            throw new ResourceConflictException("Insufficient stock for this variant");
        }

        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);
        cartRepository.flush();

        return buildCartResponse(cartRepository.findByUserId(userId).orElseThrow());
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Cart cart = cartItem.getCart();
        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
    }

    private CartResponseDto buildCartResponse(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return emptyCartResponse();
        }

        List<CartItemResponseDto> items = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            BigDecimal unitPrice = variant.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            
            boolean available = Boolean.TRUE.equals(variant.getProduct().getActive()) && 
                                variant.getStockQuantity() >= item.getQuantity();

            CartItemResponseDto itemDto = new CartItemResponseDto(
                    item.getId(),
                    variant.getProduct().getId(),
                    variant.getProduct().getName(),
                    variant.getProduct().getImageUrl(),
                    variant.getId(),
                    variant.getSize(),
                    variant.getColor(),
                    item.getQuantity(),
                    unitPrice,
                    lineTotal,
                    available
            );
            
            items.add(itemDto);
            cartTotal = cartTotal.add(lineTotal);
            totalQuantity += item.getQuantity();
        }

        return new CartResponseDto(items, cartTotal, totalQuantity);
    }

    private CartResponseDto emptyCartResponse() {
        return new CartResponseDto(Collections.emptyList(), BigDecimal.ZERO, 0);
    }
}
