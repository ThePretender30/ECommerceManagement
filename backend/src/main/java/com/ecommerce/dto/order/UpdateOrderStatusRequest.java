package com.ecommerce.dto.order;

import com.ecommerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotNull(message = "New status is required")
        OrderStatus status,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
