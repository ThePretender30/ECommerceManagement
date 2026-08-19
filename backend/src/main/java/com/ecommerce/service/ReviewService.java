package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.review.ReviewRequest;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.dto.review.ReviewSummaryResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductReview;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductReviewRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Product reviews.
 *
 * <p>The rule that gives reviews their value: a customer may only review a product they
 * have actually received. {@code OrderRepository.hasUserPurchasedProduct} checks for a
 * DELIVERED order containing the product, so ratings cannot be manufactured by someone who
 * never bought the item.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        requireProductExists(productId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PagedResponse.from(reviews, ReviewResponse::from);
    }

    /**
     * Rating summary for the product detail page.
     *
     * @param userId the signed-in user, or null for an anonymous visitor
     */
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long productId, Long userId) {
        Product product = requireProductExists(productId);

        // Distribution across the five star values, always with all five keys present so
        // the UI can render empty bars rather than missing rows.
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            breakdown.put(stars, 0L);
        }
        reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, Pageable.unpaged())
                .forEach(review -> breakdown.merge(review.getRating(), 1L, Long::sum));

        ReviewResponse ownReview = null;
        boolean canReview = false;

        if (userId != null) {
            ownReview = reviewRepository.findByProductIdAndUserId(productId, userId)
                    .map(ReviewResponse::from)
                    .orElse(null);
            // Eligible only if they bought and received it, and have not already reviewed it.
            canReview = ownReview == null && orderRepository.hasUserPurchasedProduct(userId, productId);
        }

        return new ReviewSummaryResponse(
                productId,
                product.getAverageRating(),
                reviewRepository.countByProductId(productId),
                breakdown,
                canReview,
                ownReview
        );
    }

    /** Creates a review, or updates the caller's existing one for the same product. */
    @Transactional
    public ReviewResponse submitReview(Long userId, Long productId, ReviewRequest request) {
        Product product = requireProductExists(productId);

        if (!orderRepository.hasUserPurchasedProduct(userId, productId)) {
            throw new BadRequestException(
                    "You can only review products from an order that has been delivered to you.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> ProductReview.builder()
                        .product(product)
                        .user(user)
                        .build());

        boolean isNew = review.getId() == null;

        review.setRating(request.rating());
        review.setReviewText(request.reviewText() == null || request.reviewText().isBlank()
                ? null : request.reviewText().trim());

        ProductReview saved = reviewRepository.save(review);

        recalculateProductRating(productId);

        log.info("User {} {} a review for product {}", userId, isNew ? "created" : "updated", productId);
        return ReviewResponse.from(saved);
    }

    /** Lets a customer withdraw their own review. */
    @Transactional
    public void deleteOwnReview(Long userId, Long productId) {
        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not reviewed this product."));

        reviewRepository.delete(review);
        reviewRepository.flush();   // ensure the row is gone before recomputing the average
        recalculateProductRating(productId);
        log.info("User {} deleted their review for product {}", userId, productId);
    }

    /**
     * Rewrites the product's cached {@code averageRating} and {@code reviewCount}.
     *
     * <p>Called after every review change, which is what keeps the denormalised values on
     * {@link Product} exactly consistent with the underlying rows - the reason listings can
     * sort and filter by rating without an expensive join.
     */
    private void recalculateProductRating(Long productId) {
        Double average = reviewRepository.calculateAverageRating(productId);
        long count = reviewRepository.countByProductId(productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        product.setAverageRating(average == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount((int) count);

        productRepository.save(product);
    }

    private Product requireProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }
}
