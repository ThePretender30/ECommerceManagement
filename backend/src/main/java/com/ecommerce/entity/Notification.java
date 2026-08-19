package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An audit record of every notification the system tried to deliver.
 *
 * <p>A row is written whether the send succeeded, failed, or was skipped because Twilio
 * credentials are absent. That means order flows stay fully inspectable and testable on a
 * machine with no WhatsApp account at all, and the admin can always see what was sent.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user", columnList = "user_id"),
        @Index(name = "idx_notifications_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_notifications_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_notifications_order"))
    private Order order;

    /** Delivery medium, e.g. WHATSAPP. Kept as a column so other channels can be added. */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String channel = "WHATSAPP";

    /** Destination phone number in E.164 form. */
    @Column(nullable = false, length = 30)
    private String recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** The order status that triggered this message. */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_status", length = 30)
    private OrderStatus triggerStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    /** Twilio message SID when the send succeeded. */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
