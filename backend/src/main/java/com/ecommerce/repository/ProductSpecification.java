package com.ecommerce.repository;

import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.entity.Product;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> withFilters(ProductFilterRequest filter, boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            String q = filter.normalisedQuery();
            if (q != null) {
                String pattern = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("brand"), "")), pattern)
                ));
            }

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

            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("category", JoinType.LEFT);
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
