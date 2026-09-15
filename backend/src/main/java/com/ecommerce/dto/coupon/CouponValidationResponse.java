package com.ecommerce.dto.coupon;

import com.ecommerce.entity.DiscountType;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal originalSubtotal,
        BigDecimal finalTotal,
        String message
) {
    public static CouponValidationResponse valid(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal discountAmount,
            BigDecimal originalSubtotal,
            BigDecimal finalTotal
    ) {
        return new CouponValidationResponse(
                true,
                code,
                description,
                discountType,
                discountValue,
                discountAmount,
                originalSubtotal,
                finalTotal,
                "Coupon applied successfully!"
        );
    }

    public static CouponValidationResponse invalid(String code, String message) {
        return new CouponValidationResponse(
                false,
                code,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                message
        );
    }
}
