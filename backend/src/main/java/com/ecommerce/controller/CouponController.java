package com.ecommerce.controller;

import com.ecommerce.dto.coupon.CouponValidationResponse;
import com.ecommerce.dto.coupon.ValidateCouponRequest;
import com.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validate promo codes for checkout")
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon code against a cart subtotal")
    public ResponseEntity<CouponValidationResponse> validate(@Valid @RequestBody ValidateCouponRequest request) {
        CouponValidationResponse response = couponService.validateCoupon(request.code(), request.subtotal());
        return ResponseEntity.ok(response);
    }
}
