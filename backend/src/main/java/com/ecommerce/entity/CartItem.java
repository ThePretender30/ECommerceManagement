package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A product line inside a cart.
 *
 * <p>The unique constraint on (cart_id, product_id) is what makes "add the same product
 * twice" increment a quantity instead of creating a duplicate row - the database enforces
 * the rule even if a concurrent request slips past the service-layer check.
 *
 * <p>No price is stored here: the cart always reflects the product's <em>current</em>
 * price. Prices are frozen only at checkout, on {@link OrderItem}.
 */
@Entity
@Table(name = "cart_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_cart_items_cart_product", columnNames = {"cart_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cart_items_product"))
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    /** Current price x quantity, computed on demand - never persisted. */
    @Transient
    public BigDecimal getLineTotal() {
        if (product == null || product.getPrice() == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
