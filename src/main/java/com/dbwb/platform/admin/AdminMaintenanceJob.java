package com.dbwb.platform.admin;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** BR-ADM-004: automatically reactivates temporary suspensions past their scheduled reactivation time. */
@Component
public class AdminMaintenanceJob {

    private final AdminService adminService;

    public AdminMaintenanceJob(AdminService adminService) {
        this.adminService = adminService;
    }

    @Scheduled(fixedRateString = "${dbwb.business-rules.maintenance-job-interval-ms}")
    public void run() {
        adminService.reactivateExpiredTemporarySuspensions();
    }
}
