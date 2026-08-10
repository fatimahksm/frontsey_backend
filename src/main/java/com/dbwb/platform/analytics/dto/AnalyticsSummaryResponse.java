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
        Map<String, Long> visitsByDeviceType,
        /**
         * One entry per calendar day in the range, in order, including days with
         * no visits. Gaps are filled here rather than in the client so a chart
         * cannot silently compress a quiet week into a shorter axis.
         */
        List<DailyVisitCount> visitsByDay
) {
    public record ItemViewCount(UUID itemId, String itemName, long views) {
    }

    /** {@code date} is ISO-8601 (yyyy-MM-dd). */
    public record DailyVisitCount(String date, long visits) {
    }
}
