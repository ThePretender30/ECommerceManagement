package com.ecommerce.notification;

import com.ecommerce.entity.OrderStatus;

/**
 * The message a customer receives for each order event.
 *
 * <p>Kept in one class so the wording can be reviewed and changed without touching order
 * logic, and so every status is guaranteed to have a message.
 */
public final class WhatsAppMessageTemplates {

    private WhatsAppMessageTemplates() {
    }

    /**
     * Builds the notification body for a status change.
     *
     * @param customerName the recipient's name, used to personalise the greeting
     * @param orderNumber  the human-facing order reference
     * @param status       the status just reached
     */
    public static String forStatus(String customerName, String orderNumber, OrderStatus status) {
        String firstName = firstName(customerName);

        return switch (status) {
            case ORDER_PLACED -> """
                    Hi %s, thank you for shopping with us!

                    Your order #%s has been placed successfully. We will notify you as soon as it is confirmed."""
                    .formatted(firstName, orderNumber);

            case ORDER_CONFIRMED -> """
                    Hi %s, good news!

                    Your order #%s has been confirmed and is being prepared for shipment."""
                    .formatted(firstName, orderNumber);

            case PROCESSING -> """
                    Hi %s, your order #%s is now being processed and packed.

                    We will let you know the moment it is dispatched."""
                    .formatted(firstName, orderNumber);

            case DISPATCHED -> """
                    Hi %s, your order #%s has been dispatched and is on its way.

                    You will receive another notification when it is out for delivery."""
                    .formatted(firstName, orderNumber);

            case OUT_FOR_DELIVERY -> """
                    Hi %s, your order #%s is out for delivery and should reach you today.

                    Please keep your phone nearby so our delivery partner can contact you."""
                    .formatted(firstName, orderNumber);

            case DELIVERED -> """
                    Hi %s, your order #%s has been delivered.

                    We hope you love it! Do leave a review to help other shoppers."""
                    .formatted(firstName, orderNumber);

            case CANCELLED -> """
                    Hi %s, your order #%s has been cancelled.

                    Any amount paid will be refunded to the original payment method. Contact support if you need help."""
                    .formatted(firstName, orderNumber);
        };
    }

    /** Uses just the first name so the greeting reads naturally. */
    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
