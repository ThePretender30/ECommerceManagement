package com.ecommerce.dto.order;

import com.ecommerce.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Full order detail, used by the confirmation page, My Orders detail, tracking, and the
 * admin order screen.
 *
 * <p>{@code cancellable} is computed from the status state machine rather than being
 * re-derived in the UI, so the Cancel button can never appear for an order the backend
 * would refuse to cancel.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        String status,
        String statusLabel,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        int totalQuantity,
        String deliveryFullName,
        String deliveryPhone,
        String deliveryAddress,
        Instant placedAt,
        Instant updatedAt,
        boolean cancellable,
        List<StatusHistoryResponse> statusHistory,
        // Populated for admin views only.
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone
) {
    /** Customer-facing view: no customer identity fields (the caller is the customer). */
    public static OrderResponse from(Order order) {
        return build(order, false);
    }

    /** Admin-facing view: includes who placed the order. */
    public static OrderResponse forAdmin(Order order) {
        return build(order, true);
    }

    private static OrderResponse build(Order order, boolean includeCustomer) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        List<StatusHistoryResponse> history = order.getStatusHistory().stream()
                .sorted(Comparator.comparing(h -> h.getChangedAt() == null ? Instant.EPOCH : h.getChangedAt()))
                .map(StatusHistoryResponse::from)
                .toList();

        var user = includeCustomer ? order.getUser() : null;

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getStatus().getDisplayName(),
                order.getTotalAmount(),
                items,
                items.stream().mapToInt(OrderItemResponse::quantity).sum(),
                order.getDeliveryFullName(),
                order.getDeliveryPhone(),
                order.getDeliveryAddressLine(),
                order.getPlacedAt(),
                order.getUpdatedAt(),
                order.getStatus().isCancellableByCustomer(),
                history,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null
        );
    }
}
