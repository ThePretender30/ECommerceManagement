package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * The only way the application looks up a cart. There is deliberately no
     * "find cart by id" path exposed to controllers - the cart is always derived from
     * the authenticated user, which removes the possibility of cross-user access.
     *
     * <p>The entity graph pre-fetches items and their products so rendering a cart is one
     * query rather than N+1.
     */
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Cart> findByUserId(Long userId);
}
