package com.ecommerce.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for {@code POST /api/cart/items}.
 *
 * <p>No price field: the server always reads the current price from the product row.
 * Accepting a price from the client would let a request ask to buy a laptop for one rupee.
 */
public record AddToCartRequest(

        @NotNull(message = "Product is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Quantity cannot exceed 100 per item")
        Integer quantity
) {}
