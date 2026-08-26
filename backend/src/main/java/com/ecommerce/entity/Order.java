package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "idx_orders_user", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_placed_at", columnList = "placed_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.ORDER_PLACED;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "delivery_full_name", nullable = false, length = 120)
    private String deliveryFullName;

    @Column(name = "delivery_phone", nullable = false, length = 20)
    private String deliveryPhone;

    @Column(name = "delivery_line1", nullable = false, length = 255)
    private String deliveryLine1;

    @Column(name = "delivery_line2", length = 255)
    private String deliveryLine2;

    @Column(name = "delivery_city", nullable = false, length = 100)
    private String deliveryCity;

    @Column(name = "delivery_state", nullable = false, length = 100)
    private String deliveryState;

    @Column(name = "delivery_postal_code", nullable = false, length = 20)
    private String deliveryPostalCode;

    @Column(name = "delivery_country", nullable = false, length = 100)
    private String deliveryCountry;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<OrderStatusHistory> statusHistory = new LinkedHashSet<>();

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (placedAt == null) placedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistory history) {
        statusHistory.add(history);
        history.setOrder(this);
    }

    public String getDeliveryAddressLine() {
        StringBuilder sb = new StringBuilder(deliveryLine1);
        if (deliveryLine2 != null && !deliveryLine2.isBlank()) {
            sb.append(", ").append(deliveryLine2);
        }
        sb.append(", ").append(deliveryCity)
          .append(", ").append(deliveryState)
          .append(" ").append(deliveryPostalCode)
          .append(", ").append(deliveryCountry);
        return sb.toString();
    }
}
