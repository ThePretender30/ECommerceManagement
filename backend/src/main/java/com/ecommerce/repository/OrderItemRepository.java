package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Whether a product has ever been ordered. Drives the soft-delete decision in
     * {@code ProductService.delete} - a product with order history must be deactivated,
     * not removed, or past invoices would lose their line items.
     */
    boolean existsByProductId(Long productId);

    List<OrderItem> findByOrderId(Long orderId);
}
