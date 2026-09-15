package com.ecommerce.dto.coupon;

import com.ecommerce.entity.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank(message = "Coupon code is required")
        @Size(min = 3, max = 32, message = "Coupon code must be between 3 and 32 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Coupon code can only contain letters, numbers, hyphens and underscores")
        String code,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
        BigDecimal minOrderAmount,

        @DecimalMin(value = "0.00", message = "Maximum discount amount cannot be negative")
        BigDecimal maxDiscountAmount,

        Boolean active,

        Instant expiresAt
) {}
