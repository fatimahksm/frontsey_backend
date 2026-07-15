package com.dbwb.platform.menu;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** BR-MENU-006: releases items whose temporary-unavailability window has elapsed. */
@Component
public class MenuMaintenanceJob {

    private final MenuService menuService;

    public MenuMaintenanceJob(MenuService menuService) {
        this.menuService = menuService;
    }

    @Scheduled(fixedRateString = "${dbwb.business-rules.maintenance-job-interval-ms}")
    public void run() {
        menuService.releaseExpiredTemporaryUnavailability();
    }
}
