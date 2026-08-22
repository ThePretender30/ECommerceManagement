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
import java.util.List;
import java.util.Map;

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
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(validPage, validSize);
        Page<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PagedResponse.from(reviews, ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long productId, Long userId) {
        Product product = requireProductExists(productId);

        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            breakdown.put(stars, 0L);
        }

        List<ProductReview> allReviews = reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId, Pageable.unpaged())
                .getContent();

        for (ProductReview review : allReviews) {
            Integer rating = review.getRating();
            if (rating != null && breakdown.containsKey(rating)) {
                breakdown.put(rating, breakdown.get(rating) + 1L);
            }
        }

        ReviewResponse ownReview = null;
        boolean canReview = false;

        if (userId != null) {
            ownReview = reviewRepository.findByProductIdAndUserId(productId, userId)
                    .map(ReviewResponse::from)
                    .orElse(null);
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

    @Transactional
    public void deleteOwnReview(Long userId, Long productId) {
        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not reviewed this product."));

        reviewRepository.delete(review);
        reviewRepository.flush();
        recalculateProductRating(productId);
        log.info("User {} deleted their review for product {}", userId, productId);
    }

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
