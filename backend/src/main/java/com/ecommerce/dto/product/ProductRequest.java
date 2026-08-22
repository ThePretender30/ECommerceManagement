package com.ecommerce.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must not exceed 200 characters")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price may have at most 8 digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "Category is required")
        Long categoryId,

        @Size(max = 100, message = "Brand must not exceed 100 characters")
        String brand,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stock,

        Boolean active
) {}
