package com.ecommerce.dto.product;

import java.math.BigDecimal;

/**
 * Every browse/search/filter/sort parameter in one object, bound from the query string.
 *
 * <p>All fields are optional; each null one simply contributes no predicate to the
 * generated query. That is what lets a single endpoint serve "all products",
 * "Electronics under 5000 sorted by rating", and "search for 'kettle'".
 *
 * @param q          free-text search across name, description and brand
 * @param category   category <em>slug</em> (URL-friendly), e.g. {@code kitchen-utensils}
 * @param categoryId category id, when the caller already knows it
 * @param brand      exact brand match
 * @param minPrice   inclusive lower price bound
 * @param maxPrice   inclusive upper price bound
 * @param minRating  inclusive lower bound on average rating
 * @param inStock    when true, hides products with zero stock
 * @param sort       one of: newest, oldest, price_asc, price_desc, rating, popular, name_asc, name_desc
 */
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
    /** Query strings deliver empty strings where a caller meant "no filter". */
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
