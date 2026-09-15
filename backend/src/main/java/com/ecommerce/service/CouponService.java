package com.ecommerce.service;

import com.ecommerce.dto.coupon.CouponRequest;
import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.dto.coupon.CouponValidationResponse;
import com.ecommerce.entity.Coupon;
import com.ecommerce.entity.DiscountType;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> listAllCoupons() {
        return couponRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String cleanCode = request.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new DuplicateResourceException("Coupon code '" + cleanCode + "' already exists.");
        }

        if (request.discountType() == DiscountType.PERCENTAGE) {
            if (request.discountValue().compareTo(BigDecimal.ZERO) <= 0 ||
                request.discountValue().compareTo(new BigDecimal("100")) > 0) {
                throw new BadRequestException("Percentage discount must be between 1% and 100%.");
            }
        }

        Coupon coupon = Coupon.builder()
                .code(cleanCode)
                .description(request.description().trim())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minOrderAmount(request.minOrderAmount() != null ? request.minOrderAmount() : BigDecimal.ZERO)
                .maxDiscountAmount(request.maxDiscountAmount())
                .active(request.active() == null || request.active())
                .expiresAt(request.expiresAt())
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Created new coupon: {}", saved.getCode());
        return CouponResponse.from(saved);
    }

    @Transactional
    public CouponResponse toggleActive(Long id) {
        Coupon coupon = findCouponOrThrow(id);
        coupon.setActive(!coupon.isActive());
        Coupon saved = couponRepository.save(coupon);
        log.info("Toggled coupon {} active status to {}", coupon.getCode(), saved.isActive());
        return CouponResponse.from(saved);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findCouponOrThrow(id);
        couponRepository.delete(coupon);
        log.info("Deleted coupon: {}", coupon.getCode());
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return CouponValidationResponse.invalid(code, "Please enter a promo code.");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return CouponValidationResponse.invalid(code, "Your cart is empty.");
        }

        String cleanCode = code.trim().toUpperCase();
        var optionalCoupon = couponRepository.findByCodeIgnoreCase(cleanCode);

        if (optionalCoupon.isEmpty()) {
            return CouponValidationResponse.invalid(cleanCode, "Coupon '" + cleanCode + "' does not exist.");
        }

        Coupon coupon = optionalCoupon.get();

        if (!coupon.isActive()) {
            return CouponValidationResponse.invalid(cleanCode, "This coupon is currently inactive.");
        }

        if (coupon.isExpired()) {
            return CouponValidationResponse.invalid(cleanCode, "This coupon has expired.");
        }

        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponValidationResponse.invalid(
                    cleanCode,
                    "Minimum order value of ₹" + coupon.getMinOrderAmount() + " required to use this coupon."
            );
        }

        BigDecimal discountAmount = calculateDiscount(coupon, subtotal);
        BigDecimal finalTotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        return CouponValidationResponse.valid(
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                discountAmount,
                subtotal,
                finalTotal
        );
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null &&
                coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0 &&
                discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            // FLAT discount
            discount = coupon.getDiscountValue();
        }

        // Discount cannot exceed subtotal
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon findCouponOrThrow(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
    }
}
