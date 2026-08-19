package com.ecommerce.exception;

/**
 * Thrown when a cart or checkout operation asks for more units than exist. Maps to 409.
 *
 * <p>The message names the product and the quantity actually available, so the frontend can
 * show something useful ("Only 3 left") instead of a generic failure.
 */
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, int requested, int available) {
        super("Insufficient stock for '%s': requested %d, only %d available."
                .formatted(productName, requested, available));
        this.productName = productName;
        this.requested = requested;
        this.available = available;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
