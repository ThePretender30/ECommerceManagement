package com.ecommerce.dto.order;

import com.ecommerce.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight row for the My Orders and admin order <em>lists</em>.
 *
 * <p>Kept separate from {@link OrderResponse} so a list of 50 orders does not drag every
 * line item and status-history row across the wire.
 */
public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        String status,
        String statusLabel,
        BigDecimal totalAmount,
        int itemCount,
        String firstItemName,
        String firstItemImageUrl,
        Instant placedAt,
        boolean cancellable,
        Long customerId,
        String customerName,
        String customerEmail
) {
    public static OrderSummaryResponse from(Order order) {
        return build(order, false);
    }

    public static OrderSummaryResponse forAdmin(Order order) {
        return build(order, true);
    }

    private static OrderSummaryResponse build(Order order, boolean includeCustomer) {
        var items = order.getItems();
        var first = items.isEmpty() ? null : items.get(0);
        var user = includeCustomer ? order.getUser() : null;

        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getStatus().getDisplayName(),
                order.getTotalAmount(),
                items.size(),
                first != null ? first.getProductName() : null,
                first != null ? first.getProductImageUrl() : null,
                order.getPlacedAt(),
                order.getStatus().isCancellableByCustomer(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null
        );
    }
}
