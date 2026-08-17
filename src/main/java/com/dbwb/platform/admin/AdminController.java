package com.dbwb.platform.admin;

import com.dbwb.platform.admin.dto.AccountSummaryResponse;
import com.dbwb.platform.admin.dto.AdminDashboardResponse;
import com.dbwb.platform.admin.dto.AdminWebsiteSummaryResponse;
import com.dbwb.platform.admin.dto.AdminWebsiteUpdateRequest;
import com.dbwb.platform.admin.dto.AuditLogResponse;
import com.dbwb.platform.admin.dto.PlanUpdateRequest;
import com.dbwb.platform.admin.dto.SuspendWebsiteRequest;
import com.dbwb.platform.admin.dto.ThemeRequest;
import com.dbwb.platform.admin.dto.UpdateUserRoleRequest;
import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.plan.dto.PlanResponse;
import com.dbwb.platform.security.CurrentAccount;
import com.dbwb.platform.support.dto.SupportTicketResponse;
import com.dbwb.platform.support.entity.SupportTicketStatus;
import com.dbwb.platform.theme.dto.ThemeResponse;
import com.dbwb.platform.website.dto.WebsiteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** BRD 9.17: Super Admin console. Every method delegates its role check to AdminService.requireSuperAdmin. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final CurrentAccount currentAccount;

    public AdminController(AdminService adminService, CurrentAccount currentAccount) {
        this.adminService = adminService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.ok(adminService.getDashboard(currentAccount.get()));
    }

    @GetMapping("/users")
    public ApiResponse<List<AccountSummaryResponse>> listUsers() {
        return ApiResponse.ok(adminService.listUsers(currentAccount.get()).stream().map(AccountSummaryResponse::from).toList());
    }

    @PutMapping("/users/{accountId}/role")
    public ApiResponse<AccountSummaryResponse> updateUserRole(@PathVariable UUID accountId, @Valid @RequestBody UpdateUserRoleRequest request) {
        var account = adminService.updateUserRole(accountId, currentAccount.get(), request);
        return ApiResponse.ok(AccountSummaryResponse.from(account), "Role updated.");
    }

    @PostMapping("/users/{accountId}/disable")
    public ApiResponse<AccountSummaryResponse> disableUser(@PathVariable UUID accountId) {
        var account = adminService.disableUser(accountId, currentAccount.get());
        return ApiResponse.ok(AccountSummaryResponse.from(account), "Account disabled.");
    }

    @PostMapping("/users/{accountId}/reactivate")
    public ApiResponse<AccountSummaryResponse> reactivateUser(@PathVariable UUID accountId) {
        var account = adminService.reactivateUser(accountId, currentAccount.get());
        return ApiResponse.ok(AccountSummaryResponse.from(account), "Account reactivated.");
    }

    @GetMapping("/websites")
    public ApiResponse<List<AdminWebsiteSummaryResponse>> listWebsites() {
        return ApiResponse.ok(adminService.listWebsiteSummaries(currentAccount.get()));
    }

    @PutMapping("/websites/{websiteId}")
    public ApiResponse<WebsiteResponse> updateWebsite(@PathVariable UUID websiteId, @Valid @RequestBody AdminWebsiteUpdateRequest request) {
        var website = adminService.updateWebsiteDetails(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(WebsiteResponse.from(website), "Website updated.");
    }

    @DeleteMapping("/websites/{websiteId}")
    public ApiResponse<Void> deleteWebsite(@PathVariable UUID websiteId) {
        adminService.deleteWebsite(websiteId, currentAccount.get());
        return ApiResponse.ok(null, "Website deleted.");
    }

    @PostMapping("/websites/{websiteId}/suspend")
    public ApiResponse<WebsiteResponse> suspendWebsite(@PathVariable UUID websiteId, @Valid @RequestBody SuspendWebsiteRequest request) {
        var website = adminService.suspendWebsite(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(WebsiteResponse.from(website), "Website suspended.");
    }

    @PostMapping("/websites/{websiteId}/reactivate")
    public ApiResponse<WebsiteResponse> reactivateWebsite(@PathVariable UUID websiteId) {
        var website = adminService.reactivateWebsite(websiteId, currentAccount.get());
        return ApiResponse.ok(WebsiteResponse.from(website), "Website reactivated.");
    }

    @GetMapping("/themes")
    public ApiResponse<List<ThemeResponse>> listThemes() {
        return ApiResponse.ok(adminService.listThemes(currentAccount.get()).stream().map(ThemeResponse::from).toList());
    }

    @PostMapping("/themes")
    public ApiResponse<ThemeResponse> createTheme(@Valid @RequestBody ThemeRequest request) {
        var theme = adminService.createTheme(currentAccount.get(), request);
        return ApiResponse.ok(ThemeResponse.from(theme), "Theme created.");
    }

    @PutMapping("/themes/{themeId}")
    public ApiResponse<ThemeResponse> updateTheme(@PathVariable UUID themeId, @Valid @RequestBody ThemeRequest request) {
        var theme = adminService.updateTheme(currentAccount.get(), themeId, request);
        return ApiResponse.ok(ThemeResponse.from(theme), "Theme updated.");
    }

    @DeleteMapping("/themes/{themeId}")
    public ApiResponse<Void> deleteTheme(@PathVariable UUID themeId) {
        adminService.deleteTheme(currentAccount.get(), themeId);
        return ApiResponse.ok(null, "Theme deleted.");
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanResponse>> listPlans() {
        return ApiResponse.ok(adminService.listPlans(currentAccount.get()).stream().map(PlanResponse::from).toList());
    }

    @PutMapping("/plans/{planId}")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable UUID planId, @Valid @RequestBody PlanUpdateRequest request) {
        var plan = adminService.updatePlan(currentAccount.get(), planId, request);
        return ApiResponse.ok(PlanResponse.from(plan), "Plan updated.");
    }

    @GetMapping("/support-tickets")
    public ApiResponse<List<SupportTicketResponse>> listSupportTickets() {
        return ApiResponse.ok(adminService.listSupportTickets(currentAccount.get()).stream().map(SupportTicketResponse::from).toList());
    }

    @PutMapping("/support-tickets/{ticketId}/status")
    public ApiResponse<SupportTicketResponse> updateSupportTicketStatus(@PathVariable UUID ticketId, @RequestParam SupportTicketStatus status) {
        var ticket = adminService.updateSupportTicketStatus(currentAccount.get(), ticketId, status);
        return ApiResponse.ok(SupportTicketResponse.from(ticket), "Support ticket status updated.");
    }

    @GetMapping("/audit-log")
    public ApiResponse<List<AuditLogResponse>> listAuditLogs() {
        return ApiResponse.ok(adminService.listAuditLogs(currentAccount.get()));
    }
}
