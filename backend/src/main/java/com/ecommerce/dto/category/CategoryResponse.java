package com.ecommerce.dto.category;

import com.ecommerce.entity.Category;

/** A category as shown on the home page and in the filter sidebar. */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Long productCount
) {
    public static CategoryResponse from(Category category) {
        return from(category, null);
    }

    /** {@code productCount} is supplied only where the caller has counted it. */
    public static CategoryResponse from(Category category, Long productCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                productCount
        );
    }
}
