package com.ecommerce.dto.cart;

import com.ecommerce.entity.CartItem;

import java.math.BigDecimal;

/**
 * One line in the cart view.
 *
 * <p>{@code availableStock} and {@code stockSufficient} travel with each line so the cart
 * page can flag an item that went out of stock after it was added - before the customer
 * reaches checkout and gets a failure.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        String categoryName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        Integer availableStock,
        boolean stockSufficient
) {
    public static CartItemResponse from(CartItem item) {
        var product = item.getProduct();
        int stock = product.getStock() == null ? 0 : product.getStock();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                stock,
                stock >= item.getQuantity()
        );
    }
}
