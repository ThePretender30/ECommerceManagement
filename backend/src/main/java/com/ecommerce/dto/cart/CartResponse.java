package com.ecommerce.dto.cart;

import com.ecommerce.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        int totalQuantity,
        BigDecimal subtotal,
        BigDecimal total,
        boolean checkoutAllowed
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        BigDecimal subtotal = cart.calculateSubtotal();
        boolean allLinesInStock = !items.isEmpty() && items.stream().allMatch(CartItemResponse::stockSufficient);

        return new CartResponse(
                cart.getId(),
                items,
                items.size(),
                cart.getTotalQuantity(),
                subtotal,
                subtotal,
                allLinesInStock
        );
    }
}
