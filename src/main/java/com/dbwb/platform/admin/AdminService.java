package com.dbwb.platform.admin;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.admin.dto.AdminDashboardResponse;
import com.dbwb.platform.admin.dto.AdminWebsiteUpdateRequest;
import com.dbwb.platform.admin.dto.AuditLogResponse;
import com.dbwb.platform.admin.dto.PlanUpdateRequest;
import com.dbwb.platform.admin.dto.SuspendWebsiteRequest;
import com.dbwb.platform.admin.dto.ThemeRequest;
import com.dbwb.platform.admin.dto.UpdateUserRoleRequest;
import com.dbwb.platform.profile.entity.BusinessProfile;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.admin.dto.AdminWebsiteSummaryResponse;
import com.dbwb.platform.subscription.entity.Subscription;
import java.util.stream.Collectors;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.audit.entity.AuditLog;
import com.dbwb.platform.audit.repository.AuditLogRepository;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.subscription.entity.MockPaymentStatus;
import com.dbwb.platform.support.SupportService;
import com.dbwb.platform.support.entity.SupportTicket;
import com.dbwb.platform.support.entity.SupportTicketStatus;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.theme.entity.Theme;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BRD 9.17: platform-wide Super Admin operations. Every method here checks
 * the caller's role directly (there is no "website" to scope a
 * WebsiteAccessGuard check against, since this module is platform-wide by
 * definition) - see requireSuperAdmin.
 */
@Service
public class AdminService {

    private final AccountRepository accountRepository;
    private final BusinessProfileRepository profileRepository;
    private final BusinessWebsiteRepository websiteRepository;
    private final ThemeRepository themeRepository;
    private final ThemeConfigValidator themeConfigValidator;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MockPaymentRepository mockPaymentRepository;
    private final SupportService supportService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public AdminService(
            AccountRepository accountRepository,
            BusinessProfileRepository profileRepository,
            BusinessWebsiteRepository websiteRepository,
            ThemeRepository themeRepository,
            ThemeConfigValidator themeConfigValidator,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            MockPaymentRepository mockPaymentRepository,
            SupportService supportService,
            EmailService emailService,
            AuditService auditService,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.websiteRepository = websiteRepository;
        this.themeRepository = themeRepository;
        this.themeConfigValidator = themeConfigValidator;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mockPaymentRepository = mockPaymentRepository;
        this.supportService = supportService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
    }

    // --- BR-ADM-001: platform-wide visibility ---

    @Transactional(readOnly = true)
    public List<Account> listUsers(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<BusinessWebsite> listWebsites(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return websiteRepository.findAllWithOwner();
    }

    /**
     * The website list an admin can actually act on: each site with its owner,
     * how to reach them, how many sites they run, and what they are paying for.
     *
     * Assembled here rather than in the controller so the per-website lookups
     * happen once against maps instead of once per row - a platform-wide list
     * is exactly where an N+1 hurts.
     */
    @Transactional(readOnly = true)
    public List<AdminWebsiteSummaryResponse> listWebsiteSummaries(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        List<BusinessWebsite> websites = websiteRepository.findAllWithOwner();

        Map<UUID, Long> perOwner = websites.stream()
                .collect(Collectors.groupingBy(w -> w.getOwner().getId(), Collectors.counting()));
        Map<UUID, Subscription> subscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getWebsite() != null)
                .collect(Collectors.toMap(s -> s.getWebsite().getId(), s -> s, (a, b) -> a));
        Map<UUID, BusinessProfile> profiles = profileRepository.findAll().stream()
                .filter(p -> p.getWebsite() != null)
                .collect(Collectors.toMap(p -> p.getWebsite().getId(), p -> p, (a, b) -> a));

        return websites.stream()
                .map(w -> AdminWebsiteSummaryResponse.from(
                        w,
                        profiles.get(w.getId()),
                        subscriptions.get(w.getId()),
                        perOwner.getOrDefault(w.getOwner().getId(), 1L).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        Instant now = Instant.now();
        return new AdminDashboardResponse(
                accountRepository.count(),
                websiteRepository.count(),
                subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE),
                mockPaymentRepository.countByStatus(MockPaymentStatus.PENDING),
                subscriptionRepository.findByStatusAndEndDateBetween(
                        SubscriptionStatus.ACTIVE, now, now.plus(7, ChronoUnit.DAYS)).size(),
                mockPaymentRepository.totalRevenue());
    }

    // --- direct user management, requested alongside suspend/reactivate ---

    @Transactional
    public Account updateUserRole(UUID accountId, AuthenticatedAccount caller, UpdateUserRoleRequest request) {
        requireSuperAdmin(caller);
        Account account = loadAccount(accountId);
        account.setRole(request.role());
        auditService.record(caller.accountId(), "USER_ROLE_UPDATED", accountId + " -> " + request.role());
        return account;
    }

    /** Reuses the same disable-then-permanently-delete-after-retention lifecycle as self-service deletion (AccountService.requestDeletion). */
    @Transactional
    public Account disableUser(UUID accountId, AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        Account account = loadAccount(accountId);
        account.setStatus(AccountStatus.DISABLED_PENDING_DELETION);
        account.setDisabledAt(Instant.now());

        emailService.send(account.getEmail(), "Your account has been disabled",
                "Your account has been disabled by the platform. Contact support if you believe this is a mistake.");
        auditService.record(caller.accountId(), "USER_DISABLED_BY_ADMIN", accountId.toString());
        return account;
    }

    @Transactional
    public Account reactivateUser(UUID accountId, AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        Account account = loadAccount(accountId);
        if (account.getStatus() != AccountStatus.DISABLED_PENDING_DELETION) {
            throw new BusinessRuleViolationException("This account is not currently disabled.");
        }
        account.setStatus(AccountStatus.ACTIVE);
        account.setDisabledAt(null);
        auditService.record(caller.accountId(), "USER_REACTIVATED_BY_ADMIN", accountId.toString());
        return account;
    }

    // --- BR-ADM-002..005: suspend / reactivate ---

    @Transactional
    public BusinessWebsite suspendWebsite(UUID websiteId, AuthenticatedAccount caller, SuspendWebsiteRequest request) {
        requireSuperAdmin(caller);
        BusinessWebsite website = loadWebsite(websiteId);

        website.setStatus(request.permanent() ? WebsiteStatus.SUSPENDED_PERMANENT : WebsiteStatus.SUSPENDED_TEMPORARY);
        website.setSuspensionReason(request.reason());
        website.setSuspensionReactivateAt(request.permanent() ? null : request.reactivateAt());

        emailService.send(website.getOwner().getEmail(), "Your website has been suspended",
                "\"" + website.getBusinessName() + "\" has been suspended by the platform. Contact support for details.");
        auditService.record(caller.accountId(), "WEBSITE_SUSPENDED", websiteId.toString());
        return website;
    }

    @Transactional
    public BusinessWebsite reactivateWebsite(UUID websiteId, AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        BusinessWebsite website = loadWebsite(websiteId);

        if (website.getStatus() != WebsiteStatus.SUSPENDED_TEMPORARY && website.getStatus() != WebsiteStatus.SUSPENDED_PERMANENT) {
            throw new BusinessRuleViolationException("This website is not currently suspended.");
        }
        website.setStatus(website.getPublishedContent() != null ? WebsiteStatus.PUBLISHED : WebsiteStatus.DRAFT);
        website.setSuspensionReason(null);
        website.setSuspensionReactivateAt(null);

        emailService.send(website.getOwner().getEmail(), "Your website has been reactivated",
                "\"" + website.getBusinessName() + "\" is available again.");
        auditService.record(caller.accountId(), "WEBSITE_REACTIVATED", websiteId.toString());
        return website;
    }

    /** Direct edit, beyond suspend/reactivate - deliberately narrow (just the display name) since deeper edits (slug, template type) have wider blast radius on SEO/analytics/content. */
    @Transactional
    public BusinessWebsite updateWebsiteDetails(UUID websiteId, AuthenticatedAccount caller, AdminWebsiteUpdateRequest request) {
        requireSuperAdmin(caller);
        BusinessWebsite website = loadWebsite(websiteId);
        website.setBusinessName(request.businessName());
        auditService.record(caller.accountId(), "WEBSITE_UPDATED_BY_ADMIN", websiteId.toString());
        return website;
    }

    @Transactional
    public void deleteWebsite(UUID websiteId, AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        BusinessWebsite website = loadWebsite(websiteId);
        website.setStatus(WebsiteStatus.DELETED);

        emailService.send(website.getOwner().getEmail(), "Your website has been deleted",
                "\"" + website.getBusinessName() + "\" has been permanently deleted by the platform.");
        auditService.record(caller.accountId(), "WEBSITE_DELETED_BY_ADMIN", websiteId.toString());
    }

    /** Called by the same scheduled job class as subscription maintenance - automatic reactivation for temporary suspensions. */
    @Transactional
    public void reactivateExpiredTemporarySuspensions() {
        Instant now = Instant.now();
        websiteRepository.findAll().stream()
                .filter(w -> w.getStatus() == WebsiteStatus.SUSPENDED_TEMPORARY)
                .filter(w -> w.getSuspensionReactivateAt() != null && w.getSuspensionReactivateAt().isBefore(now))
                .forEach(w -> {
                    w.setStatus(w.getPublishedContent() != null ? WebsiteStatus.PUBLISHED : WebsiteStatus.DRAFT);
                    w.setSuspensionReason(null);
                    w.setSuspensionReactivateAt(null);
                });
    }

    // --- BR-ADM-006: theme management ---

    @Transactional(readOnly = true)
    public List<Theme> listThemes(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return themeRepository.findAll();
    }

    @Transactional
    public Theme createTheme(AuthenticatedAccount caller, ThemeRequest request) {
        requireSuperAdmin(caller);
        Theme theme = new Theme();
        applyThemeRequest(theme, request);
        themeRepository.save(theme);
        auditService.record(caller.accountId(), "THEME_CREATED", theme.getId() == null ? theme.getName() : theme.getId().toString());
        return theme;
    }

    @Transactional
    public Theme updateTheme(AuthenticatedAccount caller, UUID themeId, ThemeRequest request) {
        requireSuperAdmin(caller);
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
        applyThemeRequest(theme, request);
        auditService.record(caller.accountId(), "THEME_UPDATED", themeId.toString());
        return theme;
    }

    @Transactional
    public void deleteTheme(AuthenticatedAccount caller, UUID themeId) {
        requireSuperAdmin(caller);
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
        themeRepository.delete(theme);
        auditService.record(caller.accountId(), "THEME_DELETED", themeId.toString());
    }

    // --- BR-ADM-007: plan editing (no new plan types in MVP) ---

    @Transactional(readOnly = true)
    public List<Plan> listPlans(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return planRepository.findAll();
    }

    @Transactional
    public Plan updatePlan(AuthenticatedAccount caller, UUID planId, PlanUpdateRequest request) {
        requireSuperAdmin(caller);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found."));

        plan.setPrice(request.price());
        plan.setMaxWebsites(request.maxWebsites());
        plan.setMaxManagersPerWebsite(request.maxManagersPerWebsite());
        plan.setMaxLanguages(request.maxLanguages());
        plan.setMaxGalleryImages(request.maxGalleryImages());
        plan.setImageStorageLimitMb(request.imageStorageLimitMb());
        plan.setAnalyticsEnabled(request.analyticsEnabled());
        plan.setMultiPageEnabled(request.multiPageEnabled());
        plan.setActive(request.active());

        auditService.record(caller.accountId(), "PLAN_UPDATED", planId.toString());
        return plan;
    }

    // --- BRD 9.15: support ticket triage ---

    @Transactional(readOnly = true)
    public List<SupportTicket> listSupportTickets(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return supportService.listAll();
    }

    @Transactional
    public SupportTicket updateSupportTicketStatus(AuthenticatedAccount caller, UUID ticketId, SupportTicketStatus status) {
        requireSuperAdmin(caller);
        return supportService.updateStatus(ticketId, status);
    }

    // --- BR-AUD-001: audit trail visibility ---

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAuditLogs(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();

        Map<UUID, String> emailByAccountId = new HashMap<>();
        accountRepository.findAllById(logs.stream().map(AuditLog::getActorAccountId).distinct().toList())
                .forEach(account -> emailByAccountId.put(account.getId(), account.getEmail()));

        return logs.stream()
                .map(log -> AuditLogResponse.from(log, emailByAccountId.get(log.getActorAccountId())))
                .toList();
    }

    private void applyThemeRequest(Theme theme, ThemeRequest request) {
        // Reject invalid/arbitrary theme JSON up front (Phase 3) - the public renderer relies on themeConfig
        // always parsing into the strongly-typed ThemeConfig schema, so no invalid config should ever be stored.
        themeConfigValidator.parseAndValidate(request.themeConfig());
        theme.setName(request.name());
        theme.setDescription(request.description());
        theme.setThemeConfig(request.themeConfig());
        theme.setActive(request.active());
    }

    private BusinessWebsite loadWebsite(UUID websiteId) {
        return websiteRepository.findById(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found."));
    }

    private Account loadAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    private void requireSuperAdmin(AuthenticatedAccount caller) {
        if (caller.role() != Role.SUPER_ADMIN) {
            throw new AccessDeniedForTenantException("This action is restricted to Super Admins.");
        }
    }
}
