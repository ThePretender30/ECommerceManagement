package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByPlacedAtDesc(Long userId, Pageable pageable);

    /** Ownership-scoped detail lookup used by every customer-facing order endpoint. */
    @EntityGraph(attributePaths = {"items", "statusHistory"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /** Admin detail lookup - no ownership restriction, but the endpoint requires ROLE_ADMIN. */
    @EntityGraph(attributePaths = {"items", "statusHistory", "user"})
    Optional<Order> findWithDetailsById(Long id);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByStatusOrderByPlacedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findAllByOrderByPlacedAtDesc(Pageable pageable);

    /**
     * Review eligibility: true only if this user has a DELIVERED order containing
     * this product. Spans orders and order_items, which is why it lives here rather
     * than as a database constraint.
     */
    @Query("""
            SELECT COUNT(oi) > 0 FROM OrderItem oi
            WHERE oi.order.user.id = :userId
              AND oi.product.id    = :productId
              AND oi.order.status  = com.ecommerce.entity.OrderStatus.DELIVERED
            """)
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    // ---------------------------------------------------------------------
    //  Admin statistics - real aggregates, computed by the database
    // ---------------------------------------------------------------------

    /** Revenue excludes cancelled orders. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.ecommerce.entity.OrderStatus.CANCELLED")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> com.ecommerce.entity.OrderStatus.CANCELLED AND o.placedAt >= :since")
    BigDecimal calculateRevenueSince(@Param("since") Instant since);

    long countByStatus(OrderStatus status);

    long countByPlacedAtAfter(Instant since);

    /** One row per status, so the dashboard needs a single query rather than seven. */
    @Query("SELECT o.status AS status, COUNT(o) AS count FROM Order o GROUP BY o.status")
    List<StatusCount> countGroupedByStatus();

    /** Best sellers by units shipped, ignoring cancelled orders. */
    @Query("""
            SELECT oi.product.id AS productId,
                   oi.productName AS productName,
                   SUM(oi.quantity) AS unitsSold,
                   SUM(oi.lineTotal) AS revenue
            FROM OrderItem oi
            WHERE oi.order.status <> com.ecommerce.entity.OrderStatus.CANCELLED
              AND oi.product IS NOT NULL
            GROUP BY oi.product.id, oi.productName
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<TopProduct> findTopSellingProducts(Pageable pageable);

    /** Spring Data projection - no DTO class needed for these read-only aggregates. */
    interface StatusCount {
        OrderStatus getStatus();
        Long getCount();
    }

    interface TopProduct {
        Long getProductId();
        String getProductName();
        Long getUnitsSold();
        BigDecimal getRevenue();
    }
}
