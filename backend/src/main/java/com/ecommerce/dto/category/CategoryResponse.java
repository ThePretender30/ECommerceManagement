package com.ecommerce.dto.category;

import com.ecommerce.entity.Category;

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
