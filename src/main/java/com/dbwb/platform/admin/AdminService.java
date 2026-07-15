package com.dbwb.platform.admin;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.admin.dto.AdminDashboardResponse;
import com.dbwb.platform.admin.dto.PlanUpdateRequest;
import com.dbwb.platform.admin.dto.SuspendWebsiteRequest;
import com.dbwb.platform.admin.dto.ThemeRequest;
import com.dbwb.platform.audit.AuditService;
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
import java.util.List;
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
    private final BusinessWebsiteRepository websiteRepository;
    private final ThemeRepository themeRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MockPaymentRepository mockPaymentRepository;
    private final SupportService supportService;
    private final EmailService emailService;
    private final AuditService auditService;

    public AdminService(
            AccountRepository accountRepository,
            BusinessWebsiteRepository websiteRepository,
            ThemeRepository themeRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            MockPaymentRepository mockPaymentRepository,
            SupportService supportService,
            EmailService emailService,
            AuditService auditService) {
        this.accountRepository = accountRepository;
        this.websiteRepository = websiteRepository;
        this.themeRepository = themeRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mockPaymentRepository = mockPaymentRepository;
        this.supportService = supportService;
        this.emailService = emailService;
        this.auditService = auditService;
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

    private void applyThemeRequest(Theme theme, ThemeRequest request) {
        theme.setName(request.name());
        theme.setDescription(request.description());
        theme.setThemeConfig(request.themeConfig());
        theme.setActive(request.active());
    }

    private BusinessWebsite loadWebsite(UUID websiteId) {
        return websiteRepository.findById(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found."));
    }

    private void requireSuperAdmin(AuthenticatedAccount caller) {
        if (caller.role() != Role.SUPER_ADMIN) {
            throw new AccessDeniedForTenantException("This action is restricted to Super Admins.");
        }
    }
}
