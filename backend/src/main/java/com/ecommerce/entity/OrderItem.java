package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One purchased line within an order.
 *
 * <p><b>Why name and price are duplicated here:</b> {@code productName} and
 * {@code unitPrice} are frozen at the moment of checkout. If an admin later raises a
 * product's price or renames it, this order must still show what the customer actually
 * agreed to pay. The {@code product} reference is kept for "buy it again" links and
 * review eligibility, and is nullable so a hard-deleted product cannot destroy history.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    /** Nullable on purpose - history survives product deletion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_order_items_product"))
    private Product product;

    /** Snapshot of the product name at purchase time. */
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    /** Snapshot of the price paid per unit. */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /** Persisted so historical totals never shift if rounding rules change. */
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
