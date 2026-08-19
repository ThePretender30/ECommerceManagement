package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A sellable item.
 *
 * <p>{@code averageRating} and {@code reviewCount} are denormalised copies of what could be
 * computed from {@code product_reviews}. They are stored because product listings sort and
 * filter by rating on every page load; recomputing an AVG across all reviews for every row
 * of every listing would be wasteful. {@code ReviewService} recalculates them whenever a
 * review is written, so they never drift.
 *
 * <p>{@code active} supports soft deletion: a product referenced by historical orders is
 * deactivated rather than removed, which keeps past invoices intact.
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_date_added", columnList = "date_added")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** DECIMAL(10,2) - never a floating point type, to avoid rounding errors on money. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(length = 100)
    private String brand;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "date_added", nullable = false, updatable = false)
    private Instant dateAdded;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    void onCreate() {
        if (dateAdded == null) {
            dateAdded = Instant.now();
        }
    }

    public boolean isInStock() {
        return stock != null && stock > 0;
    }

    public boolean hasStockFor(int quantity) {
        return stock != null && stock >= quantity;
    }
}
