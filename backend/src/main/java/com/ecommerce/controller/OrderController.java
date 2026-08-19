package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.dto.order.OrderTrackingResponse;
import com.ecommerce.dto.order.PlaceOrderRequest;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Checkout, order history and tracking for the signed-in customer.
 *
 * <p>Every lookup is scoped to {@code principal.getId()} in the service layer, so
 * requesting another customer's order id returns 404 rather than their data.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place orders and track your own order history")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order from your cart")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(principal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "List your orders, newest first")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> myOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getUserOrders(principal.getId(), page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one of your orders in full")
    public ResponseEntity<OrderResponse> getOrder(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getUserOrder(principal.getId(), id));
    }

    /** Status plus the full timeline, with each stage marked completed/current/pending. */
    @GetMapping("/{id}/tracking")
    @Operation(summary = "Track one of your orders")
    public ResponseEntity<OrderTrackingResponse> track(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id) {
        return ResponseEntity.ok(orderService.trackOrder(principal.getId(), id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of your orders (only before it is dispatched)")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long id,
                                                @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(orderService.cancelOrder(principal.getId(), id, reason));
    }
}
