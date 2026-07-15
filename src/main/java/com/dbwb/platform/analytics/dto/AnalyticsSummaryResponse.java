package com.dbwb.platform.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** BR-AN-001: total visits, most-viewed items, referral source, device type, over a date range. */
public record AnalyticsSummaryResponse(
        Instant from,
        Instant to,
        long totalVisits,
        List<ItemViewCount> mostViewedItems,
        Map<String, Long> visitsByReferralSource,
        Map<String, Long> visitsByDeviceType
) {
    public record ItemViewCount(UUID itemId, String itemName, long views) {
    }
}
