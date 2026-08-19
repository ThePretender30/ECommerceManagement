package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * {@code JpaSpecificationExecutor} is the important part here: browsing, category
 * filtering, keyword search, price/rating/brand filters and sorting are all served by a
 * single dynamic query built in {@code ProductSpecification}, instead of a combinatorial
 * explosion of {@code findByCategoryAndPriceBetweenAndBrand...} methods.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findTop8ByActiveTrueOrderByDateAddedDesc();

    List<Product> findTop8ByActiveTrueOrderByAverageRatingDescReviewCountDesc();

    List<Product> findTop8ByActiveTrueAndReviewCountGreaterThanOrderByReviewCountDesc(Integer minReviews);

    long countByCategoryId(Long categoryId);

    /** Distinct brand list for a category, used to populate the filter sidebar. */
    @Query("""
            SELECT DISTINCT p.brand FROM Product p
            WHERE p.active = true AND p.brand IS NOT NULL AND p.brand <> ''
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            ORDER BY p.brand
            """)
    List<String> findDistinctBrands(@Param("categoryId") Long categoryId);

    /** Products at or below the given stock level - drives the admin low-stock panel. */
    List<Product> findByActiveTrueAndStockLessThanEqualOrderByStockAsc(Integer threshold);

    long countByActiveTrueAndStockLessThanEqual(Integer threshold);

    long countByActiveTrue();
}
