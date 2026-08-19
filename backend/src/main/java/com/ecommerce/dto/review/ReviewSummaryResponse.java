package com.ecommerce.dto.review;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregate rating information shown above the review list on a product page.
 *
 * @param ratingBreakdown star value (1-5) to number of reviews, for the distribution bars
 * @param canReview       true when the signed-in user has a delivered order with this
 *                        product and has not reviewed it yet; false for guests
 * @param userReview      the caller's existing review, when they have one
 */
public record ReviewSummaryResponse(
        Long productId,
        BigDecimal averageRating,
        long totalReviews,
        Map<Integer, Long> ratingBreakdown,
        boolean canReview,
        ReviewResponse userReview
) {}
