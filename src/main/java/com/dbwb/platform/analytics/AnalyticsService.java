package com.dbwb.platform.analytics;

import com.dbwb.platform.analytics.dto.AnalyticsSummaryResponse;
import com.dbwb.platform.analytics.entity.AnalyticsEvent;
import com.dbwb.platform.analytics.entity.AnalyticsEventType;
import com.dbwb.platform.analytics.entity.DeviceType;
import com.dbwb.platform.analytics.repository.AnalyticsEventRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.website.WebsiteAccessGuard;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** BRD 9.16: first-party visit/item-view tracking and the Owner analytics dashboard. */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final WebsiteAccessGuard accessGuard;
    private final SubscriptionQueryService subscriptionQueryService;
    private final BusinessRuleProperties businessRules;

    public AnalyticsService(
            AnalyticsEventRepository repository,
            MenuItemRepository menuItemRepository,
            WebsiteAccessGuard accessGuard,
            SubscriptionQueryService subscriptionQueryService,
            BusinessRuleProperties businessRules) {
        this.repository = repository;
        this.menuItemRepository = menuItemRepository;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
        this.businessRules = businessRules;
    }

    /**
     * Discards events past the retention window.
     *
     * The table had none: a row per visit and per item view, kept forever, on
     * a path with no other bound. Retention is configured rather than fixed
     * here (dbwb.business-rules.analytics-event-retention-days), and a value of
     * zero or less disables the purge entirely rather than deleting everything
     * - a missing setting must not silently destroy a customer's history.
     */
    @Transactional
    public int purgeExpiredEvents() {
        int retentionDays = businessRules.getAnalyticsEventRetentionDays();
        if (retentionDays <= 0) {
            return 0;
        }
        return repository.deleteOlderThan(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
    }

    /**
     * BR-AN-002: every visit is counted, including Owner/Manager traffic - no
     * exclusion in MVP.
     *
     * Written on a background thread rather than in the request. A visitor
     * waiting on a page should not also wait on an INSERT nobody is going to
     * read for days, and an analytics table under load should slow the numbers
     * down, not the pages.
     *
     * Losing the odd row if the process dies mid-write is an accepted trade: a
     * visit count is a trend, not a ledger. Anything that had to balance would
     * not belong on this path.
     */
    @Async
    @Transactional
    public void recordPageView(UUID websiteId, String referralSource, DeviceType deviceType) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setWebsiteId(websiteId);
        event.setEventType(AnalyticsEventType.PAGE_VIEW);
        event.setReferralSource(referralSource);
        event.setDeviceType(deviceType);
        repository.save(event);
    }

    /** Same reasoning as recordPageView: off the request, and cheap to lose. */
    @Async
    @Transactional
    public void recordItemView(UUID websiteId, UUID itemId, DeviceType deviceType) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setWebsiteId(websiteId);
        event.setEventType(AnalyticsEventType.ITEM_VIEW);
        event.setItemId(itemId);
        event.setDeviceType(deviceType);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(UUID websiteId, AuthenticatedAccount caller, Instant from, Instant to) {
        accessGuard.requirePermission(websiteId, caller, Permission.VIEW_ANALYTICS);
        requireAnalyticsEnabledPlan(websiteId);

        long totalVisits = repository.countByWebsiteIdAndEventTypeAndCreatedAtBetween(
                websiteId, AnalyticsEventType.PAGE_VIEW, from, to);

        var mostViewed = repository.mostViewedItems(websiteId, from, to).stream()
                .limit(10)
                .map(row -> {
                    UUID itemId = UUID.fromString(row.getKey());
                    String name = menuItemRepository.findById(itemId).map(i -> i.getName()).orElse("(deleted item)");
                    return new AnalyticsSummaryResponse.ItemViewCount(itemId, name, row.getTotal());
                })
                .toList();

        Map<String, Long> byReferralSource = new LinkedHashMap<>();
        repository.visitsByReferralSource(websiteId, from, to).forEach(row -> byReferralSource.put(row.getKey(), row.getTotal()));

        Map<String, Long> byDeviceType = new LinkedHashMap<>();
        repository.visitsByDeviceType(websiteId, from, to).forEach(row -> byDeviceType.put(row.getKey(), row.getTotal()));

        return new AnalyticsSummaryResponse(
                from, to, totalVisits, mostViewed, byReferralSource, byDeviceType, dailyVisits(websiteId, from, to));
    }

    /**
     * Daily visit counts across the whole range, quiet days included.
     *
     * The query only returns days that had traffic; a chart drawn straight from
     * that would put Monday next to Friday and read as continuous. Filling the
     * gaps here keeps every consumer - the dashboard, a future export - honest
     * by default.
     */
    private List<AnalyticsSummaryResponse.DailyVisitCount> dailyVisits(UUID websiteId, Instant from, Instant to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        repository.visitsByDay(websiteId, from, to).forEach(row -> counts.put(row.getKey(), row.getTotal()));

        List<AnalyticsSummaryResponse.DailyVisitCount> days = new ArrayList<>();
        LocalDate cursor = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate last = to.atZone(ZoneOffset.UTC).toLocalDate();
        // A range longer than a year is not a trend line, it is a wall; cap the
        // series rather than returning something no chart can draw.
        int guard = 0;
        while (!cursor.isAfter(last) && guard++ < 366) {
            String key = cursor.toString();
            days.add(new AnalyticsSummaryResponse.DailyVisitCount(key, counts.getOrDefault(key, 0L)));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    /**
     * BR-AN-003: CSV rather than a binary XLSX/PDF - Excel/Sheets/Numbers all
     * open CSV natively. True PDF/XLSX generation needs a reporting library
     * decision (TBD-009) that hasn't been made yet.
     */
    @Transactional(readOnly = true)
    public String exportSummaryCsv(UUID websiteId, AuthenticatedAccount caller, Instant from, Instant to) {
        AnalyticsSummaryResponse summary = getSummary(websiteId, caller, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Report period,").append(summary.from()).append(",").append(summary.to()).append("\n");
        csv.append("Total visits,").append(summary.totalVisits()).append("\n\n");

        csv.append("Most viewed items\nItem,Views\n");
        summary.mostViewedItems().forEach(item -> csv.append(csvEscape(item.itemName())).append(",").append(item.views()).append("\n"));

        csv.append("\nVisits by referral source\nSource,Visits\n");
        summary.visitsByReferralSource().forEach((source, count) -> csv.append(csvEscape(source)).append(",").append(count).append("\n"));

        csv.append("\nVisits by device type\nDevice,Visits\n");
        summary.visitsByDeviceType().forEach((device, count) -> csv.append(csvEscape(device)).append(",").append(count).append("\n"));

        return csv.toString();
    }

    private void requireAnalyticsEnabledPlan(UUID websiteId) {
        boolean enabled = subscriptionQueryService.getActivePlan(websiteId)
                .map(plan -> plan.isAnalyticsEnabled())
                .orElse(false);
        if (!enabled) {
            throw new BusinessRuleViolationException("Analytics is not available on the current plan.");
        }
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return value.contains(",") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    /** Simple User-Agent heuristic - good enough for the MVP breakdown (BR-AN-001); not a full UA-parsing library. */
    public static DeviceType classifyDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.UNKNOWN;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return DeviceType.TABLET;
        }
        if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }
}
