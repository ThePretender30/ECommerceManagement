package com.ecommerce.dto.order;

import com.ecommerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin payload for {@code PUT /api/admin/orders/{id}/status}.
 *
 * <p>The requested status is checked against {@link OrderStatus#canTransitionTo} before
 * anything is written, so the admin UI cannot push an order into an impossible state.
 */
public record UpdateOrderStatusRequest(

        @NotNull(message = "New status is required")
        OrderStatus status,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
