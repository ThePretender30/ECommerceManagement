package com.ecommerce.dto.admin;

import com.ecommerce.entity.Notification;

import java.time.Instant;

/**
 * A notification delivery record for the admin audit screen.
 *
 * <p>Because a row is written for skipped and failed attempts too, this view answers
 * "did the customer actually hear about this?" rather than only listing successes.
 */
public record NotificationResponse(
        Long id,
        String channel,
        String recipient,
        String message,
        String triggerStatus,
        String status,
        String providerMessageId,
        String errorMessage,
        Instant createdAt,
        Long orderId,
        String orderNumber,
        Long userId,
        String userName
) {
    public static NotificationResponse from(Notification notification) {
        var order = notification.getOrder();
        var user = notification.getUser();
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getMessage(),
                notification.getTriggerStatus() != null ? notification.getTriggerStatus().name() : null,
                notification.getStatus().name(),
                notification.getProviderMessageId(),
                notification.getErrorMessage(),
                notification.getCreatedAt(),
                order != null ? order.getId() : null,
                order != null ? order.getOrderNumber() : null,
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null
        );
    }
}
