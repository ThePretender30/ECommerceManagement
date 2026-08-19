package com.ecommerce.dto.cart;

import com.ecommerce.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

/**
 * The whole cart, with every monetary figure computed on the server.
 *
 * <p>{@code checkoutAllowed} is the single answer to "can this cart be ordered right now",
 * so the Cart and Checkout pages cannot disagree about it.
 *
 * @param subtotal  sum of all line totals
 * @param total     final payable amount (equals subtotal - no shipping or tax modelled)
 */
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
