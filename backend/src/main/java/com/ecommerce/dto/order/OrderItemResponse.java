package com.ecommerce.dto.order;

import com.ecommerce.entity.OrderItem;

import java.math.BigDecimal;

/**
 * One purchased line.
 *
 * <p>Every field here is read from the order's own snapshot columns, not from the live
 * product - which is why a past order still shows the price the customer actually paid.
 * {@code productAvailable} tells the UI whether a "buy again" link can be offered.
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal,
        boolean productAvailable
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductName(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getProduct() != null && item.getProduct().isActive()
        );
    }
}
