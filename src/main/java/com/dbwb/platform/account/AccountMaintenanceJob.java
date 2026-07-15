package com.dbwb.platform.account;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** BR-AUTH-006: permanently deletes accounts whose disable window has elapsed. */
@Component
public class AccountMaintenanceJob {

    private final AccountService accountService;

    public AccountMaintenanceJob(AccountService accountService) {
        this.accountService = accountService;
    }

    @Scheduled(fixedRateString = "${dbwb.business-rules.maintenance-job-interval-ms}")
    public void run() {
        accountService.permanentlyDeleteOverdueAccounts();
    }
}
