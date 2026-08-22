package com.ecommerce.dto.order;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record OrderTrackingResponse(
        Long orderId,
        String orderNumber,
        String currentStatus,
        String currentStatusLabel,
        Instant placedAt,
        Instant lastUpdatedAt,
        boolean cancelled,
        boolean delivered,
        List<TrackingStep> progressSteps,
        List<StatusHistoryResponse> history
) {

    public record TrackingStep(
            String status,
            String label,
            String state,
            Instant occurredAt
    ) {}

    private static final List<OrderStatus> HAPPY_PATH = List.of(
            OrderStatus.ORDER_PLACED,
            OrderStatus.ORDER_CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.DISPATCHED,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
    );

    public static OrderTrackingResponse from(Order order) {
        List<StatusHistoryResponse> history = order.getStatusHistory().stream()
                .sorted(Comparator.comparing(h -> h.getChangedAt() == null ? Instant.EPOCH : h.getChangedAt()))
                .map(StatusHistoryResponse::from)
                .toList();

        Map<String, Instant> reachedAt = order.getStatusHistory().stream()
                .collect(java.util.stream.Collectors.toMap(
                        h -> h.getStatus().name(),
                        h -> h.getChangedAt() == null ? Instant.EPOCH : h.getChangedAt(),
                        (earliest, later) -> earliest));

        OrderStatus current = order.getStatus();
        boolean cancelled = current == OrderStatus.CANCELLED;

        List<TrackingStep> steps;
        if (cancelled) {
            steps = List.of(
                    new TrackingStep(OrderStatus.ORDER_PLACED.name(),
                            OrderStatus.ORDER_PLACED.getDisplayName(), "completed",
                            reachedAt.get(OrderStatus.ORDER_PLACED.name())),
                    new TrackingStep(OrderStatus.CANCELLED.name(),
                            OrderStatus.CANCELLED.getDisplayName(), "current",
                            reachedAt.get(OrderStatus.CANCELLED.name()))
            );
        } else {
            int currentIndex = HAPPY_PATH.indexOf(current);
            steps = HAPPY_PATH.stream().map(stage -> {
                int stageIndex = HAPPY_PATH.indexOf(stage);
                String state = stageIndex < currentIndex ? "completed"
                             : stageIndex == currentIndex ? "current"
                             : "pending";
                return new TrackingStep(stage.name(), stage.getDisplayName(), state,
                        reachedAt.get(stage.name()));
            }).toList();
        }

        return new OrderTrackingResponse(
                order.getId(),
                order.getOrderNumber(),
                current.name(),
                current.getDisplayName(),
                order.getPlacedAt(),
                order.getUpdatedAt(),
                cancelled,
                current == OrderStatus.DELIVERED,
                steps,
                history
        );
    }
}
