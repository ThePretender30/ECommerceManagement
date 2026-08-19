package com.ecommerce.repository;

import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the dynamic WHERE clause for product browsing.
 *
 * <p>This class is why the product API needs only one repository method. Each filter that
 * the caller actually supplied contributes one predicate; the rest are skipped. Browsing
 * everything, filtering Electronics under 5000 with a 4-star minimum, and searching for
 * "kettle" are all the same query with different predicate sets - no
 * {@code findByCategoryAndBrandAndPriceBetween...} explosion.
 *
 * <p>Every value is bound as a JPA Criteria parameter, never concatenated into SQL, so
 * these filters cannot be used for SQL injection.
 */
public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> withFilters(ProductFilterRequest filter, boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Customers never see soft-deleted products; the admin list passes true.
            if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            // Free-text search across name, description and brand.
            String q = filter.normalisedQuery();
            if (q != null) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), pattern)
                ));
            }

            // Category by id takes precedence; otherwise match the URL slug.
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            } else if (filter.normalisedCategory() != null) {
                predicates.add(cb.equal(
                        cb.lower(root.get("category").get("slug")),
                        filter.normalisedCategory().toLowerCase()));
            }

            if (filter.normalisedBrand() != null) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), filter.normalisedBrand().toLowerCase()));
            }

            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }

            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), filter.minRating()));
            }

            if (Boolean.TRUE.equals(filter.inStock())) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            // Fetching the category alongside avoids an N+1 query when mapping to DTOs.
            // Only safe on the content query - Spring Data reuses this spec for the
            // COUNT query, where a join fetch is illegal.
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.LEFT);
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
