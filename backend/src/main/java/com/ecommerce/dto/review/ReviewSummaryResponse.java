package com.ecommerce.dto.review;

import java.math.BigDecimal;
import java.util.Map;

public record ReviewSummaryResponse(
        Long productId,
        BigDecimal averageRating,
        long totalReviews,
        Map<Integer, Long> ratingBreakdown,
        boolean canReview,
        ReviewResponse userReview
) {}
