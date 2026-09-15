package com.ecommerce.controller;

import com.ecommerce.dto.coupon.CouponRequest;
import com.ecommerce.dto.coupon.CouponResponse;
import com.ecommerce.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Coupon management (ROLE_ADMIN, protected under /api/admin/**). */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Admin - Coupons", description = "Create and manage discount coupons (ROLE_ADMIN)")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "List all coupons")
    public ResponseEntity<List<CouponResponse>> list() {
        return ResponseEntity.ok(couponService.listAllCoupons());
    }

    @PostMapping
    @Operation(summary = "Create a new coupon")
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle active status of a coupon")
    public ResponseEntity<CouponResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a coupon")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }
}
