package com.ecommerce.dto.admin;

import com.ecommerce.dto.order.OrderSummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The admin dashboard payload. Every figure is a real aggregate computed by the database -
 * none of it is placeholder data.
 *
 * @param ordersByStatus     status name to count, driving the status breakdown chart
 * @param topSellingProducts best sellers by units shipped, cancelled orders excluded
 * @param lowStockProducts   products at or below the low-stock threshold
 */
public record AdminStatsResponse(
        BigDecimal totalRevenue,
        BigDecimal revenueLast30Days,
        long totalOrders,
        long ordersLast30Days,
        long pendingOrders,
        long deliveredOrders,
        long cancelledOrders,
        long totalCustomers,
        long newCustomersLast30Days,
        long totalProducts,
        long lowStockCount,
        long totalCategories,
        Map<String, Long> ordersByStatus,
        List<TopSellingProduct> topSellingProducts,
        List<LowStockProduct> lowStockProducts,
        List<OrderSummaryResponse> recentOrders
) {

    public record TopSellingProduct(
            Long productId,
            String productName,
            long unitsSold,
            BigDecimal revenue
    ) {}

    public record LowStockProduct(
            Long productId,
            String productName,
            String categoryName,
            int stock
    ) {}
}
