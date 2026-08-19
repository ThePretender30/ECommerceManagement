package com.ecommerce.service;

import com.ecommerce.dto.address.AddressRequest;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.order.*;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.notification.OrderStatusChangedEvent;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Checkout and the order lifecycle.
 *
 * <p>The two things worth understanding in this class:
 *
 * <ol>
 *   <li><b>Checkout is one atomic transaction.</b> Stock validation, stock decrement,
 *       order creation and cart clearing either all happen or none do. If the last unit of
 *       any item was sold while the customer was on the checkout page, the whole thing
 *       rolls back and nothing is half-written.</li>
 *   <li><b>Notifications are events, not calls.</b> Status changes publish an
 *       {@link OrderStatusChangedEvent} instead of invoking the WhatsApp sender directly.
 *       The listener runs after commit, on another thread - see
 *       {@code OrderNotificationListener} for why that ordering matters.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter ORDER_NUMBER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================================================================
    //  Checkout
    // ==================================================================

    /**
     * Converts the caller's cart into an order.
     *
     * <p>Prices and quantities come from the server-side cart, never from the request, so
     * a tampered payload cannot change what is charged. Every line is re-validated against
     * live stock here even though {@code CartService} already checked - between adding to
     * the cart and paying, another customer may have taken the last unit.
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {

        if (!request.hasExactlyOneAddressSource()) {
            throw new BadRequestException(
                    "Provide either an existing addressId or a new delivery address, but not both.");
        }

        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty. Add something before checking out.");
        }

        User user = cart.getUser();

        // ---- Resolve the delivery address ----------------------------
        Address deliveryAddress;
        if (request.addressId() != null) {
            deliveryAddress = addressService.findOwnedAddressOrThrow(userId, request.addressId());
        } else {
            deliveryAddress = buildTransientAddress(user, request.newAddress());
            if (Boolean.TRUE.equals(request.saveNewAddress())) {
                addressService.create(userId, request.newAddress());
            }
        }

        // ---- Build the order, validating and decrementing stock ------
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.ORDER_PLACED)
                .totalAmount(BigDecimal.ZERO)
                .addressId(deliveryAddress.getId())
                .deliveryFullName(deliveryAddress.getFullName())
                .deliveryPhone(deliveryAddress.getPhone())
                .deliveryLine1(deliveryAddress.getLine1())
                .deliveryLine2(deliveryAddress.getLine2())
                .deliveryCity(deliveryAddress.getCity())
                .deliveryState(deliveryAddress.getState())
                .deliveryPostalCode(deliveryAddress.getPostalCode())
                .deliveryCountry(deliveryAddress.getCountry())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            // Re-read the product inside this transaction to get its committed stock.
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product", cartItem.getProduct().getId()));

            if (!product.isActive()) {
                throw new BadRequestException(
                        "'%s' is no longer available. Please remove it from your cart."
                                .formatted(product.getName()));
            }

            int quantity = cartItem.getQuantity();
            if (!product.hasStockFor(quantity)) {
                // Rolls back everything done so far in this transaction.
                throw new InsufficientStockException(
                        product.getName(), quantity, product.getStock() == null ? 0 : product.getStock());
            }

            // Prices are frozen here - later price edits will not alter this order.
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(product.getImageUrl())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .lineTotal(lineTotal)
                    .build();
            order.addItem(orderItem);

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);

        // First entry in the tracking timeline.
        order.addStatusHistory(OrderStatusHistory.builder()
                .status(OrderStatus.ORDER_PLACED)
                .note("Order placed by customer.")
                .build());

        Order saved = orderRepository.save(order);

        // The cart has become an order; empty it.
        cart.getItems().clear();

        log.info("Order {} placed by user {} for {} item(s), total {}",
                saved.getOrderNumber(), userId, saved.getItems().size(), total);

        publishStatusEvent(saved, null, OrderStatus.ORDER_PLACED);

        return OrderResponse.from(saved);
    }

    // ==================================================================
    //  Customer queries
    // ==================================================================

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<Order> orders = orderRepository.findByUserIdOrderByPlacedAtDesc(userId, pageable);
        return PagedResponse.from(orders, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(Long userId, Long orderId) {
        return OrderResponse.from(findOwnedOrderOrThrow(userId, orderId));
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrder(Long userId, Long orderId) {
        return OrderTrackingResponse.from(findOwnedOrderOrThrow(userId, orderId));
    }

    /**
     * Customer-initiated cancellation.
     *
     * <p>Only legal before dispatch, and it restores the stock it took. After dispatch the
     * goods have physically left, so returning them to inventory automatically would
     * corrupt the stock count - those cases go through support instead.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = findOwnedOrderOrThrow(userId, orderId);
        OrderStatus previous = order.getStatus();

        if (!previous.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BadRequestException(
                    "This order can no longer be cancelled because it is already '%s'. Please contact support."
                            .formatted(previous.getDisplayName()));
        }

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.addStatusHistory(OrderStatusHistory.builder()
                .status(OrderStatus.CANCELLED)
                .note(reason == null || reason.isBlank()
                        ? "Cancelled by customer."
                        : "Cancelled by customer: " + reason.trim())
                .build());

        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled by user {}", saved.getOrderNumber(), userId);

        publishStatusEvent(saved, previous, OrderStatus.CANCELLED);

        return OrderResponse.from(saved);
    }

    // ==================================================================
    //  Admin operations
    // ==================================================================

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getAllOrders(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<Order> orders = (status == null)
                ? orderRepository.findAllByOrderByPlacedAtDesc(pageable)
                : orderRepository.findByStatusOrderByPlacedAtDesc(status, pageable);
        return PagedResponse.from(orders, OrderSummaryResponse::forAdmin);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return OrderResponse.forAdmin(order);
    }

    /**
     * Advances an order through its lifecycle.
     *
     * <p>The requested transition is validated against {@link OrderStatus#canTransitionTo}
     * before anything is written, so an order cannot skip stages or move backwards even if
     * a malformed request asks it to.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus previous = order.getStatus();
        OrderStatus target = request.status();

        if (previous == target) {
            throw new BadRequestException(
                    "This order is already marked '%s'.".formatted(target.getDisplayName()));
        }

        if (!previous.canTransitionTo(target)) {
            throw new BadRequestException(
                    "Cannot change status from '%s' to '%s'. Allowed next steps: %s."
                            .formatted(previous.getDisplayName(), target.getDisplayName(),
                                    describeAllowed(previous)));
        }

        // An admin cancellation puts the reserved stock back, exactly as a customer one does.
        if (target == OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        order.setStatus(target);
        order.addStatusHistory(OrderStatusHistory.builder()
                .status(target)
                .note(request.note() == null || request.note().isBlank()
                        ? "Status updated by administrator."
                        : request.note().trim())
                .build());

        Order saved = orderRepository.save(order);
        log.info("Order {} moved {} -> {}", saved.getOrderNumber(), previous, target);

        publishStatusEvent(saved, previous, target);

        return OrderResponse.forAdmin(saved);
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private Order findOwnedOrderOrThrow(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    /** Returns every line's quantity to inventory. Used by both cancellation paths. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        log.debug("Restored stock for cancelled order {}", order.getOrderNumber());
    }

    /**
     * Publishes the change for the notification pipeline.
     *
     * <p>Values are copied out of the entity here because the listener runs after commit,
     * on another thread, where lazy associations would no longer be loadable.
     */
    private void publishStatusEvent(Order order, OrderStatus previous, OrderStatus current) {
        User user = order.getUser();
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getOrderNumber(),
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                previous,
                current
        ));
    }

    /** Wraps a checkout-time address that may never be saved, so it can be snapshotted. */
    private Address buildTransientAddress(User user, AddressRequest request) {
        return Address.builder()
                .user(user)
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .line1(request.line1().trim())
                .line2(request.line2() == null || request.line2().isBlank() ? null : request.line2().trim())
                .city(request.city().trim())
                .state(request.state().trim())
                .postalCode(request.postalCode().trim())
                .country(request.country() == null || request.country().isBlank()
                        ? "India" : request.country().trim())
                .build();
    }

    /**
     * Builds a readable, unique order reference such as {@code ORD-20260817-4821}.
     *
     * <p>Preferred over exposing the numeric primary key: sequential ids would let anyone
     * count how many orders the shop has taken, and guess neighbouring order numbers.
     */
    private String generateOrderNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "ORD-%s-%04d".formatted(
                    ORDER_NUMBER_DATE.format(Instant.now()),
                    ThreadLocalRandom.current().nextInt(10_000));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        // Collisions are vanishingly unlikely; fall back to a timestamp-based value.
        return "ORD-%s-%d".formatted(ORDER_NUMBER_DATE.format(Instant.now()), System.nanoTime() % 100_000);
    }

    private String describeAllowed(OrderStatus status) {
        List<String> names = status.nextStatuses().stream()
                .map(OrderStatus::getDisplayName)
                .sorted()
                .toList();
        return names.isEmpty() ? "none (this order is in a final state)" : String.join(", ", names);
    }
}
