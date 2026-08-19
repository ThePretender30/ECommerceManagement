package com.ecommerce.service;

import com.ecommerce.dto.cart.AddToCartRequest;
import com.ecommerce.dto.cart.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The shopping cart.
 *
 * <p>Every method here takes the caller's {@code userId} from the authenticated principal
 * and resolves the cart from it. No endpoint accepts a cart id, so there is no request a
 * customer could craft to read or modify someone else's cart - the ownership check is
 * structural rather than something a future endpoint might forget to write.
 *
 * <p>Stock is checked here as a courtesy so the customer finds out early, but the
 * authoritative check happens again inside the checkout transaction in
 * {@link OrderService} - between adding to a cart and paying, someone else may have bought
 * the last unit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public CartResponse getCart(Long userId) {
        return CartResponse.from(getOrCreateCart(userId));
    }

    /**
     * Adds a product, or increases its quantity if it is already in the cart.
     *
     * <p>The increment is deliberate: clicking "Add to cart" twice on a product page should
     * mean two of that item, not a second identical line.
     */
    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productService.findProductOrThrow(request.productId());

        if (!product.isActive()) {
            throw new BadRequestException("'%s' is no longer available.".formatted(product.getName()));
        }

        CartItem existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        int newQuantity = (existing == null)
                ? request.quantity()
                : existing.getQuantity() + request.quantity();

        requireStock(product, newQuantity);

        if (existing == null) {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(newQuantity)
                    .build();
            cart.addItem(item);
        } else {
            existing.setQuantity(newQuantity);
        }

        cartRepository.save(cart);
        log.debug("User {} now has {} x '{}' in their cart", userId, newQuantity, product.getName());

        return CartResponse.from(reloadCart(userId));
    }

    /** Sets an absolute quantity for one line. */
    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        // Scoped to this cart: a guessed item id belonging to someone else simply is not found.
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));

        requireStock(item.getProduct(), quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return CartResponse.from(reloadCart(userId));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));

        // orphanRemoval on Cart.items turns this into a DELETE.
        cart.removeItem(item);
        cartRepository.save(cart);

        return CartResponse.from(reloadCart(userId));
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        log.debug("Cleared cart for user {}", userId);
        return CartResponse.from(reloadCart(userId));
    }

    /**
     * Returns the user's cart, creating an empty one on first use.
     *
     * <p>Package-private overload used by {@link OrderService} at checkout, so both share
     * exactly one definition of "this user's cart".
     */
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart cart = Cart.builder().user(user).build();
            log.debug("Created cart for user {}", userId);
            return cartRepository.save(cart);
        });
    }

    /** Re-reads through the entity graph so the response includes fully loaded items. */
    private Cart reloadCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
    }

    private void requireStock(Product product, int requested) {
        if (!product.hasStockFor(requested)) {
            throw new InsufficientStockException(
                    product.getName(), requested, product.getStock() == null ? 0 : product.getStock());
        }
    }
}
