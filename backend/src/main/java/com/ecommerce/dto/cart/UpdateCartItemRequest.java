package com.ecommerce.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1. Remove the item instead of setting it to 0.")
        @Max(value = 100, message = "Quantity cannot exceed 100 per item")
        Integer quantity
) {}
