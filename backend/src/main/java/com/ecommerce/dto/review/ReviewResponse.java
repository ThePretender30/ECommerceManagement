package com.ecommerce.dto.review;

import com.ecommerce.entity.ProductReview;

import java.time.Instant;

/**
 * A published review.
 *
 * <p>Only the reviewer's display name is exposed - never their email or phone - because
 * reviews are readable by anonymous visitors.
 */
public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        Integer rating,
        String reviewText,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getReviewText(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
