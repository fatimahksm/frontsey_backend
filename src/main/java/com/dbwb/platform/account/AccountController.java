package com.dbwb.platform.account;

import com.dbwb.platform.account.dto.AccountDataExportResponse;
import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-AUTH-006/BR-DATA-005: the authenticated account's own deletion lifecycle and data export. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentAccount currentAccount;

    public AccountController(AccountService accountService, CurrentAccount currentAccount) {
        this.accountService = accountService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/data-export")
    public ApiResponse<AccountDataExportResponse> exportData() {
        return ApiResponse.ok(accountService.exportData(currentAccount.get()));
    }

    @PostMapping("/deletion/request")
    public ApiResponse<Void> requestDeletion() {
        accountService.requestDeletion(currentAccount.get());
        return ApiResponse.ok(null, "Your account will be permanently deleted after the retention window unless you cancel.");
    }

    @PostMapping("/deletion/cancel")
    public ApiResponse<Void> cancelDeletion() {
        accountService.cancelDeletion(currentAccount.get());
        return ApiResponse.ok(null, "Account deletion cancelled.");
    }
}
