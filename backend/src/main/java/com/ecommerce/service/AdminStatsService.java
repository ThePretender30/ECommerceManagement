package com.ecommerce.service;

import com.ecommerce.dto.admin.AdminStatsResponse;
import com.ecommerce.dto.order.OrderSummaryResponse;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final int RECENT_ORDER_COUNT = 8;
    private static final int TOP_PRODUCT_COUNT = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getDashboardStats() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), 0L);
        }
        for (OrderRepository.StatusCount row : orderRepository.countGroupedByStatus()) {
            ordersByStatus.put(row.getStatus().name(), row.getCount());
        }

        long totalOrders = ordersByStatus.values().stream().mapToLong(Long::longValue).sum();

        long pendingOrders = ordersByStatus.get(OrderStatus.ORDER_PLACED.name())
                + ordersByStatus.get(OrderStatus.ORDER_CONFIRMED.name())
                + ordersByStatus.get(OrderStatus.PROCESSING.name())
                + ordersByStatus.get(OrderStatus.DISPATCHED.name())
                + ordersByStatus.get(OrderStatus.OUT_FOR_DELIVERY.name());

        List<AdminStatsResponse.TopSellingProduct> topProducts =
                orderRepository.findTopSellingProducts(PageRequest.of(0, TOP_PRODUCT_COUNT)).stream()
                        .map(row -> new AdminStatsResponse.TopSellingProduct(
                                row.getProductId(),
                                row.getProductName(),
                                row.getUnitsSold() == null ? 0L : row.getUnitsSold(),
                                row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue()))
                        .toList();

        List<AdminStatsResponse.LowStockProduct> lowStock =
                productRepository.findByActiveTrueAndStockLessThanEqualOrderByStockAsc(
                                ProductService.LOW_STOCK_THRESHOLD).stream()
                        .map(product -> new AdminStatsResponse.LowStockProduct(
                                product.getId(),
                                product.getName(),
                                product.getCategory() != null ? product.getCategory().getName() : null,
                                product.getStock()))
                        .toList();

        List<OrderSummaryResponse> recentOrders =
                orderRepository.findAllByOrderByPlacedAtDesc(PageRequest.of(0, RECENT_ORDER_COUNT))
                        .map(OrderSummaryResponse::forAdmin)
                        .getContent();

        return new AdminStatsResponse(
                orderRepository.calculateTotalRevenue(),
                orderRepository.calculateRevenueSince(thirtyDaysAgo),
                totalOrders,
                orderRepository.countByPlacedAtAfter(thirtyDaysAgo),
                pendingOrders,
                ordersByStatus.get(OrderStatus.DELIVERED.name()),
                ordersByStatus.get(OrderStatus.CANCELLED.name()),
                userRepository.count(),
                userRepository.countByCreatedAtAfter(thirtyDaysAgo),
                productRepository.countByActiveTrue(),
                productRepository.countByActiveTrueAndStockLessThanEqual(ProductService.LOW_STOCK_THRESHOLD),
                categoryRepository.count(),
                ordersByStatus,
                topProducts,
                lowStock,
                recentOrders
        );
    }
}
