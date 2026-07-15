package com.dbwb.platform.admin.dto;

import java.math.BigDecimal;

/** BR-AN-004: platform-wide Super Admin dashboard figures. */
public record AdminDashboardResponse(
        long totalUsers,
        long totalWebsites,
        long activeSubscriptions,
        long pendingPayments,
        long subscriptionsExpiringSoon,
        BigDecimal totalRevenue
) {
}
