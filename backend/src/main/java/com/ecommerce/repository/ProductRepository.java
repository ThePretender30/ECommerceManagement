package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findTop8ByActiveTrueOrderByDateAddedDesc();

    List<Product> findTop8ByActiveTrueOrderByAverageRatingDescReviewCountDesc();

    List<Product> findTop8ByActiveTrueAndReviewCountGreaterThanOrderByReviewCountDesc(Integer minReviews);

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT DISTINCT p.brand FROM Product p
            WHERE p.active = true AND p.brand IS NOT NULL AND p.brand <> ''
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            ORDER BY p.brand
            """)
    List<String> findDistinctBrands(@Param("categoryId") Long categoryId);

    List<Product> findByActiveTrueAndStockLessThanEqualOrderByStockAsc(Integer threshold);

    long countByActiveTrueAndStockLessThanEqual(Integer threshold);

    long countByActiveTrue();
}
