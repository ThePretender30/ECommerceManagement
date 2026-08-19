package com.ecommerce.controller;

import com.ecommerce.dto.cart.AddToCartRequest;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.dto.cart.UpdateCartItemRequest;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * The signed-in customer's cart.
 *
 * <p>Notice that no route contains a cart id. The cart is always resolved from the
 * authenticated principal, so there is no identifier a caller could substitute to reach
 * someone else's cart.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Your shopping cart (requires authentication)")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Fetch your cart, creating an empty one on first use")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getId()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product, or increase its quantity if already present")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Set an absolute quantity for one cart line")
    public ResponseEntity<CartResponse> updateItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long itemId,
                                                   @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(
                cartService.updateItemQuantity(principal.getId(), itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove one line from your cart")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(principal.getId(), itemId));
    }

    @DeleteMapping
    @Operation(summary = "Empty your cart")
    public ResponseEntity<CartResponse> clear(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.clearCart(principal.getId()));
    }
}
