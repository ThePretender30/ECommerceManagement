package com.ecommerce.dto.product;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String q,
        String category,
        Long categoryId,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        Boolean inStock,
        String sort
) {
    public String normalisedQuery() {
        return (q == null || q.isBlank()) ? null : q.trim();
    }

    public String normalisedBrand() {
        return (brand == null || brand.isBlank()) ? null : brand.trim();
    }

    public String normalisedCategory() {
        return (category == null || category.isBlank()) ? null : category.trim();
    }
}
