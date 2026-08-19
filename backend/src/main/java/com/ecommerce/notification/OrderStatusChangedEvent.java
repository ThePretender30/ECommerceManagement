package com.ecommerce.notification;

import com.ecommerce.entity.OrderStatus;

/**
 * Published whenever an order is created or its status changes.
 *
 * <p>Carrying plain values rather than the {@code Order} entity is deliberate: the
 * listener runs after the transaction has committed and on a different thread, where a
 * detached entity's lazy collections would blow up. Everything the notification needs is
 * captured here at publish time.
 *
 * @param previousStatus null when the order was just placed
 */
public record OrderStatusChangedEvent(
        Long orderId,
        String orderNumber,
        Long userId,
        String customerName,
        String customerPhone,
        OrderStatus previousStatus,
        OrderStatus newStatus
) {}
