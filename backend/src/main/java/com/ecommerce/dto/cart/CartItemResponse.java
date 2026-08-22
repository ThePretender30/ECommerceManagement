package com.ecommerce.dto.cart;

import com.ecommerce.entity.CartItem;

import java.math.BigDecimal;

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
