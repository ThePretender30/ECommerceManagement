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

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

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
