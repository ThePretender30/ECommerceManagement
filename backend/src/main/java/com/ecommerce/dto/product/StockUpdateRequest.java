package com.ecommerce.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Payload for {@code PATCH /api/admin/products/{id}/stock}. */
public record StockUpdateRequest(

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock
) {}
