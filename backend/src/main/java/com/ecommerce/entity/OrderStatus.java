package com.ecommerce.entity;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of an order.
 *
 * <p>This enum owns the state machine rather than scattering {@code if} checks through
 * the service layer. {@link #canTransitionTo(OrderStatus)} is the single source of truth
 * for which status changes are legal, and it is enforced on every admin status update so
 * an order can never jump from, say, ORDER_PLACED straight to DELIVERED.
 */
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

    /**
     * Allowed forward transitions. DELIVERED and CANCELLED are terminal.
     *
     * <p>Cancellation is permitted only before the parcel physically leaves the
     * warehouse - once DISPATCHED, stock has already left inventory and restoring it
     * automatically would corrupt the stock count.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            ORDER_PLACED,     EnumSet.of(ORDER_CONFIRMED, CANCELLED),
            ORDER_CONFIRMED,  EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING,       EnumSet.of(DISPATCHED, CANCELLED),
            DISPATCHED,       EnumSet.of(OUT_FOR_DELIVERY),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
            DELIVERED,        Collections.emptySet(),
            CANCELLED,        Collections.emptySet()
    );

    /** @return true if {@code target} is a legal next status from this one. */
    public boolean canTransitionTo(OrderStatus target) {
        return target != null && ALLOWED.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /** @return the statuses reachable in one step from this one (never null). */
    public Set<OrderStatus> nextStatuses() {
        return Collections.unmodifiableSet(ALLOWED.getOrDefault(this, Collections.emptySet()));
    }

    /** A customer may cancel their own order only while it is still cancellable. */
    public boolean isCancellableByCustomer() {
        return canTransitionTo(CANCELLED);
    }

    /** Terminal states cannot change again. */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
