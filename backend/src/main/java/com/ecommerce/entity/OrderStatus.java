package com.ecommerce.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {

    ORDER_PLACED("Order Placed"),
    ORDER_CONFIRMED("Order Confirmed"),
    PROCESSING("Processing"),
    DISPATCHED("Dispatched"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            ORDER_PLACED,     EnumSet.of(ORDER_CONFIRMED, CANCELLED),
            ORDER_CONFIRMED,  EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING,       EnumSet.of(DISPATCHED, CANCELLED),
            DISPATCHED,       EnumSet.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
            DELIVERED,        Collections.emptySet(),
            CANCELLED,        Collections.emptySet()
    );

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && ALLOWED.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public Set<OrderStatus> nextStatuses() {
        return Collections.unmodifiableSet(ALLOWED.getOrDefault(this, Collections.emptySet()));
    }

    public boolean isCancellableByCustomer() {
        return canTransitionTo(CANCELLED);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
