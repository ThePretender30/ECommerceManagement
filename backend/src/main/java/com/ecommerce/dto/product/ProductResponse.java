package com.ecommerce.dto.product;

import com.ecommerce.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String brand,
        String imageUrl,
        Integer stock,
        boolean inStock,
        BigDecimal averageRating,
        Integer reviewCount,
        Instant dateAdded,
        boolean active
) {
    public static ProductResponse from(Product product) {
        var category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                category != null ? category.getSlug() : null,
                product.getBrand(),
                product.getImageUrl(),
                product.getStock(),
                product.isInStock(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getDateAdded(),
                product.isActive()
        );
    }
}
