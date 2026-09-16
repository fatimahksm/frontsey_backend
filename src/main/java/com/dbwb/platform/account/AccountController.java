package com.dbwb.platform.account;

import com.dbwb.platform.account.dto.AccountDataExportResponse;
import com.dbwb.platform.account.dto.AccountProfileResponse;
import com.dbwb.platform.account.dto.ChangePasswordRequest;
import com.dbwb.platform.account.dto.UpdateAccountProfileRequest;
import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The authenticated account's own profile, password, deletion lifecycle (BR-AUTH-006) and data export (BR-DATA-005). */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentAccount currentAccount;

    public AccountController(AccountService accountService, CurrentAccount currentAccount) {
        this.accountService = accountService;
        this.currentAccount = currentAccount;
    }

    /** Who you are signed in as - the screen that offers to export and delete an account should be able to name it. */
    @GetMapping("/me")
    public ApiResponse<AccountProfileResponse> me() {
        return ApiResponse.ok(accountService.profile(currentAccount.get()));
    }

    @PutMapping("/me")
    public ApiResponse<AccountProfileResponse> updateMe(@Valid @RequestBody UpdateAccountProfileRequest request) {
        return ApiResponse.ok(accountService.updateProfile(currentAccount.get(), request), "Your details were saved.");
    }

    /** Requires the current password: a live session on an unattended machine must not be enough to take an account over. */
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(currentAccount.get(), request);
        return ApiResponse.ok(null, "Your password was changed.");
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
