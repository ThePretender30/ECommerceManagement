package com.ecommerce.notification;

import com.ecommerce.entity.OrderStatus;

public record OrderStatusChangedEvent(
        Long orderId,
        String orderNumber,
        Long userId,
        String customerName,
        String customerPhone,
        OrderStatus previousStatus,
        OrderStatus newStatus
) {}
