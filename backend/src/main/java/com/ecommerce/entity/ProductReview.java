package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A customer's rating and comment for a product they bought.
 *
 * <p>The unique constraint on (product_id, user_id) enforces one review per customer per
 * product at the database level. Eligibility - the reviewer must have a DELIVERED order
 * containing this product - is checked in {@code ReviewService}, because that rule spans
 * three tables and cannot be expressed as a column constraint.
 */
@Entity
@Table(name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_product_user", columnNames = {"product_id", "user_id"}),
        indexes = @Index(name = "idx_reviews_product", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_user"))
    private User user;

    /** 1 to 5 inclusive; also validated on the incoming DTO. */
    @Column(nullable = false)
    private Integer rating;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
