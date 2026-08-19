package com.ecommerce.dto.order;

import com.ecommerce.entity.OrderStatusHistory;

import java.time.Instant;

/** One step in the order tracking timeline. */
public record StatusHistoryResponse(
        Long id,
        String status,
        String statusLabel,
        String note,
        Instant changedAt
) {
    public static StatusHistoryResponse from(OrderStatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(),
                history.getStatus().name(),
                history.getStatus().getDisplayName(),
                history.getNote(),
                history.getChangedAt()
        );
    }
}
