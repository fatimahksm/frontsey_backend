package com.dbwb.platform.manager;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs on the shared maintenance interval - expires stale PENDING manager invitations (BR-MGR-007). */
@Component
public class ManagerMaintenanceJob {

    private final ManagerService managerService;

    public ManagerMaintenanceJob(ManagerService managerService) {
        this.managerService = managerService;
    }

    @Scheduled(fixedRateString = "${dbwb.business-rules.maintenance-job-interval-ms}")
    public void run() {
        managerService.runExpiryMaintenance();
    }
}
