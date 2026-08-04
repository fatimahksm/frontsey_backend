package com.dbwb.platform.analytics;

import com.dbwb.platform.analytics.dto.AnalyticsSummaryResponse;
import com.dbwb.platform.analytics.entity.AnalyticsEvent;
import com.dbwb.platform.analytics.entity.AnalyticsEventType;
import com.dbwb.platform.analytics.entity.DeviceType;
import com.dbwb.platform.analytics.repository.AnalyticsEventRepository;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.website.WebsiteAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** BRD 9.16: first-party visit/item-view tracking and the Owner analytics dashboard. */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;
    private final MenuItemRepository menuItemRepository;
    private final WebsiteAccessGuard accessGuard;
    private final SubscriptionQueryService subscriptionQueryService;

    public AnalyticsService(
            AnalyticsEventRepository repository,
            MenuItemRepository menuItemRepository,
            WebsiteAccessGuard accessGuard,
            SubscriptionQueryService subscriptionQueryService) {
        this.repository = repository;
        this.menuItemRepository = menuItemRepository;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    /** BR-AN-002: every visit is counted, including Owner/Manager traffic - no exclusion in MVP. */
    @Transactional
    public void recordPageView(UUID websiteId, String referralSource, DeviceType deviceType) {
        AnalyticsEvent event = new AnalyticsEvent();
        event.setWebsiteId(websiteId);
        event.setEventType(AnalyticsEventType.PAGE_VIEW);
        event.setReferralSource(referralSource);
        event.setDeviceType(deviceType);
        repository.save(event);
    }

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

        return new AnalyticsSummaryResponse(from, to, totalVisits, mostViewed, byReferralSource, byDeviceType);
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
