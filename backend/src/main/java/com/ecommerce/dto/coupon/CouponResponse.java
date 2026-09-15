package com.ecommerce.dto.coupon;

import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        boolean active,
        Instant expiresAt,
        boolean expired,
        Instant createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.isActive(),
                coupon.getExpiresAt(),
                coupon.isExpired(),
                coupon.getCreatedAt()
        );
    }
}
