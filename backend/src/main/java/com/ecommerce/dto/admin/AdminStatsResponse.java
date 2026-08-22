package com.ecommerce.dto.admin;

import com.ecommerce.dto.order.OrderSummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
