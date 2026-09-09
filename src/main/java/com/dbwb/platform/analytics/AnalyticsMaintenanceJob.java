package com.dbwb.platform.analytics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps analytics_events bounded - it is written to on every public page load and had no retention at all. */
@Component
public class AnalyticsMaintenanceJob {

    private final AnalyticsService analyticsService;

    public AnalyticsMaintenanceJob(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Scheduled(fixedRateString = "${dbwb.business-rules.maintenance-job-interval-ms}")
    public void run() {
        analyticsService.purgeExpiredEvents();
    }
}
