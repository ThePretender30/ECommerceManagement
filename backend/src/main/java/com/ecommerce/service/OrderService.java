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

        Address deliveryAddress;
        if (request.addressId() != null) {
            deliveryAddress = addressService.findOwnedAddressOrThrow(userId, request.addressId());
        } else {
            deliveryAddress = buildTransientAddress(user, request.newAddress());
            if (Boolean.TRUE.equals(request.saveNewAddress())) {
                addressService.create(userId, request.newAddress());
            }
        }

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
                throw new InsufficientStockException(
                        product.getName(), quantity, product.getStock() == null ? 0 : product.getStock());
            }

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

        order.addStatusHistory(OrderStatusHistory.builder()
                .status(OrderStatus.ORDER_PLACED)
                .note("Order placed by customer.")
                .build());

        Order saved = orderRepository.save(order);

        cart.getItems().clear();

        log.info("Order {} placed by user {} for {} item(s), total {}",
                saved.getOrderNumber(), userId, saved.getItems().size(), total);

        publishStatusEvent(saved, null, OrderStatus.ORDER_PLACED);

        return OrderResponse.from(saved);
    }

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

    private Order findOwnedOrderOrThrow(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

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

    private String generateOrderNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "ORD-%s-%04d".formatted(
                    ORDER_NUMBER_DATE.format(Instant.now()),
                    ThreadLocalRandom.current().nextInt(10_000));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
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
