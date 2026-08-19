package com.ecommerce.controller;

import com.ecommerce.dto.admin.NotificationResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.OrderResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.dto.order.UpdateOrderStatusRequest;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.service.NotificationService;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Order management (ROLE_ADMIN).
 *
 * <p>Changing a status here is what triggers the customer's WhatsApp notification - the
 * update publishes an event, and the notification is delivered after the transaction
 * commits. See {@code OrderNotificationListener}.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Orders", description = "View all orders and advance their status (ROLE_ADMIN)")
public class AdminOrderController {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List all orders, optionally filtered by status")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(status, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one order in full, including customer details")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForAdmin(id));
    }

    /**
     * Advances an order. Rejected with a 400 explaining the allowed next steps if the
     * transition is not legal from the current status.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Change an order's status and notify the customer on WhatsApp")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    /** Which transitions the UI should offer for a given status. */
    @GetMapping("/statuses")
    @Operation(summary = "List every status and the transitions allowed from it")
    public ResponseEntity<List<Map<String, Object>>> statuses() {
        List<Map<String, Object>> statuses = java.util.Arrays.stream(OrderStatus.values())
                .map(status -> Map.<String, Object>of(
                        "value", status.name(),
                        "label", status.getDisplayName(),
                        "terminal", status.isTerminal(),
                        "allowedNext", status.nextStatuses().stream().map(Enum::name).sorted().toList()))
                .toList();
        return ResponseEntity.ok(statuses);
    }

    /** Delivery attempts for this order, so the admin can see if the customer was told. */
    @GetMapping("/{id}/notifications")
    @Operation(summary = "List notification attempts for one order")
    public ResponseEntity<List<NotificationResponse>> notifications(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.listForOrder(id));
    }
}
