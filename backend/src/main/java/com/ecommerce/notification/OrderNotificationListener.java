package com.ecommerce.notification;

import com.ecommerce.entity.Notification;
import com.ecommerce.entity.NotificationStatus;
import com.ecommerce.repository.NotificationRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns an {@link OrderStatusChangedEvent} into a WhatsApp message and an audit row.
 *
 * <p><b>Why {@code AFTER_COMMIT} plus {@code @Async}, and why it matters:</b>
 * <ul>
 *   <li><b>AFTER_COMMIT</b> - the message is sent only once the order is durably saved.
 *       A plain listener would fire inside the checkout transaction, so a later rollback
 *       would leave the customer holding a WhatsApp message for an order that does not
 *       exist.</li>
 *   <li><b>@Async</b> - delivery happens on a background thread. Twilio is a network call
 *       that can take seconds or hang; running it inline would make the customer wait for
 *       an external service before their "order placed" page appeared.</li>
 * </ul>
 *
 * <p>Together these guarantee the notification can never delay, block or roll back an order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final WhatsAppService whatsAppService;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // REQUIRES_NEW because the original transaction is already committed and gone.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            String message = WhatsAppMessageTemplates.forStatus(
                    event.customerName(), event.orderNumber(), event.newStatus());

            WhatsAppService.SendResult result = whatsAppService.send(event.customerPhone(), message);

            persistRecord(event, message, result);

        } catch (Exception ex) {
            // Nothing above should throw, but a notification failure must never escape
            // and mark the async task as failed - the order itself is already safe.
            log.error("Failed to process notification for order {}: {}",
                    event.orderNumber(), ex.getMessage(), ex);
        }
    }

    /** Writes the audit row for every attempt, whether it was sent, failed or skipped. */
    private void persistRecord(OrderStatusChangedEvent event, String message,
                               WhatsAppService.SendResult result) {

        NotificationStatus status = switch (result.outcome()) {
            case SENT -> NotificationStatus.SENT;
            case FAILED -> NotificationStatus.FAILED;
            case SKIPPED -> NotificationStatus.SKIPPED;
        };

        Notification notification = Notification.builder()
                .user(userRepository.findById(event.userId()).orElse(null))
                .order(orderRepository.findById(event.orderId()).orElse(null))
                .channel("WHATSAPP")
                .recipient(event.customerPhone() == null ? "unknown" : event.customerPhone())
                .message(message)
                .triggerStatus(event.newStatus())
                .status(status)
                .providerMessageId(result.providerMessageId())
                .errorMessage(result.errorMessage())
                .build();

        notificationRepository.save(notification);

        log.debug("Recorded {} notification for order {} ({} -> {})",
                status, event.orderNumber(), event.previousStatus(), event.newStatus());
    }
}
