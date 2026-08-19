package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One append-only entry in an order's status timeline.
 *
 * <p>{@link Order#getStatus()} answers "where is my order now"; this table answers
 * "how did it get there and when". Real order tracking needs the second question
 * answered, which is why a single status column on the order is not enough.
 */
@Entity
@Table(name = "order_status_history", indexes =
        @Index(name = "idx_osh_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_osh_order"))
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    /** Optional free-text note from the admin who made the change. */
    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
