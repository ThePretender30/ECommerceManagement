package com.ecommerce.controller;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.dto.review.ReviewRequest;
import com.ecommerce.dto.review.ReviewResponse;
import com.ecommerce.dto.review.ReviewSummaryResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public product browsing, plus the review endpoints that hang off a product.
 *
 * <p>Reads here are anonymous-friendly - a storefront that demands a login to look at the
 * catalogue is useless. Writing a review requires authentication, enforced by
 * {@code SecurityConfig}'s {@code anyRequest().authenticated()} rule.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse, search, filter and review products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    /**
     * The single browse/search/filter/sort endpoint.
     *
     * <p>Every parameter is optional, so this one URL serves the full catalogue, a category
     * page, a search result and any combination of filters.
     */
    @GetMapping
    @Operation(summary = "List products with optional search, filters, sorting and pagination")
    public ResponseEntity<PagedResponse<ProductResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        var filter = new ProductFilterRequest(
                q, category, categoryId, brand, minPrice, maxPrice, minRating, inStock, sort);

        return ResponseEntity.ok(productService.search(filter, page, size, false));
    }

    @GetMapping("/featured")
    @Operation(summary = "Top-rated products for the home page")
    public ResponseEntity<List<ProductResponse>> featured() {
        return ResponseEntity.ok(productService.getFeatured());
    }

    @GetMapping("/popular")
    @Operation(summary = "Most-reviewed products for the home page")
    public ResponseEntity<List<ProductResponse>> popular() {
        return ResponseEntity.ok(productService.getPopular());
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Most recently added products")
    public ResponseEntity<List<ProductResponse>> newArrivals() {
        return ResponseEntity.ok(productService.getNewArrivals());
    }

    @GetMapping("/brands")
    @Operation(summary = "Distinct brands, optionally scoped to a category, for filter controls")
    public ResponseEntity<List<String>> brands(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(productService.getBrands(categoryId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one product's full detail")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // ---------------- Reviews for a product ----------------

    @GetMapping("/{id}/reviews")
    @Operation(summary = "List a product's reviews (public)")
    public ResponseEntity<PagedResponse<ReviewResponse>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getProductReviews(id, page, size));
    }

    /**
     * Rating summary. The principal is optional: for a signed-in user the response also
     * says whether they are eligible to review, and includes their existing review.
     */
    @GetMapping("/{id}/reviews/summary")
    @Operation(summary = "Average rating, distribution, and the caller's review eligibility")
    public ResponseEntity<ReviewSummaryResponse> reviewSummary(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(reviewService.getSummary(id, userId));
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Write or update your review (requires a delivered order with this product)")
    public ResponseEntity<ReviewResponse> submitReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submitReview(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}/reviews/mine")
    @Operation(summary = "Delete your own review of this product")
    public ResponseEntity<Void> deleteOwnReview(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.deleteOwnReview(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
