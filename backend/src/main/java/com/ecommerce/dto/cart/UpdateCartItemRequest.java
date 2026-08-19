package com.ecommerce.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code PUT /api/cart/items/{itemId}} - sets an absolute quantity.
 *
 * <p>Absolute rather than a delta so that a retried or duplicated request is idempotent:
 * sending "quantity = 3" twice still means three.
 */
public record UpdateCartItemRequest(

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1. Remove the item instead of setting it to 0.")
        @Max(value = 100, message = "Quantity cannot exceed 100 per item")
        Integer quantity
) {}
