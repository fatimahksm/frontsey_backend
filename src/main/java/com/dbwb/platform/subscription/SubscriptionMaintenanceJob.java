package com.dbwb.platform.subscription;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs hourly - frequent enough that grace/expiry transitions (BR-SUB-006/008) are timely without being wasteful. */
@Component
public class SubscriptionMaintenanceJob {

    private final SubscriptionService subscriptionService;

    public SubscriptionMaintenanceJob(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void run() {
        subscriptionService.runLifecycleMaintenance();
    }
}
