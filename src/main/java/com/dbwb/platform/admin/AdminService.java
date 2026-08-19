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
import com.dbwb.platform.admin.dto.AdminPlatformReportResponse;
import com.dbwb.platform.subscription.entity.MockPayment;
import com.dbwb.platform.website.entity.LayoutVariant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import com.dbwb.platform.admin.dto.ProvisionWebsiteRequest;
import com.dbwb.platform.admin.dto.ProvisionedWebsiteResponse;
import com.dbwb.platform.account.entity.AccountToken;
import com.dbwb.platform.account.entity.TokenType;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.SlugGenerator;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.config.FrontendProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dbwb.platform.plan.dto.TemplatePriceResponse;
import com.dbwb.platform.plan.dto.TemplatePriceUpdateRequest;
import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
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
    private final AccountTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlugGenerator slugGenerator;
    private final BusinessRuleProperties businessRules;
    private final String frontendBaseUrl;
    private final BusinessProfileRepository profileRepository;
    private final BusinessWebsiteRepository websiteRepository;
    private final ThemeRepository themeRepository;
    private final ThemeConfigValidator themeConfigValidator;
    private final PlanRepository planRepository;
    private final TemplatePriceRepository templatePriceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MockPaymentRepository mockPaymentRepository;
    private final SupportService supportService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    public AdminService(
            AccountRepository accountRepository,
            AccountTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            SlugGenerator slugGenerator,
            BusinessRuleProperties businessRules,
            FrontendProperties frontendProperties,
            BusinessProfileRepository profileRepository,
            BusinessWebsiteRepository websiteRepository,
            ThemeRepository themeRepository,
            ThemeConfigValidator themeConfigValidator,
            PlanRepository planRepository,
            TemplatePriceRepository templatePriceRepository,
            SubscriptionRepository subscriptionRepository,
            MockPaymentRepository mockPaymentRepository,
            SupportService supportService,
            EmailService emailService,
            AuditService auditService,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.slugGenerator = slugGenerator;
        this.businessRules = businessRules;
        this.frontendBaseUrl = frontendProperties.getPublicSiteBaseUrl();
        this.profileRepository = profileRepository;
        this.websiteRepository = websiteRepository;
        this.themeRepository = themeRepository;
        this.themeConfigValidator = themeConfigValidator;
        this.planRepository = planRepository;
        this.templatePriceRepository = templatePriceRepository;
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
     * Stands a website up on an owner's behalf.
     *
     * The owner is named by email because the point is that they may not have
     * an account yet. If they do, it is reused - never a second account for the
     * same person. If they do not, one is created and they are invited to set
     * their own password: the admin picks a throwaway that is immediately
     * unusable, never chooses the real one, and never sees it. Completing that
     * invitation both sets the password and verifies the address, so the owner
     * signs in once rather than chasing two emails.
     *
     * `complimentary` grants free access outright rather than faking a payment:
     * an ACTIVE subscription with no end date, so the site publishes, never
     * expires, and is never billed.
     */
    @Transactional
    public ProvisionedWebsiteResponse provisionWebsiteForOwner(
            AuthenticatedAccount caller, ProvisionWebsiteRequest request) {
        requireSuperAdmin(caller);

        String email = request.ownerEmail().trim();
        Account owner = accountRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean created = owner == null;

        if (created) {
            owner = new Account();
            owner.setEmail(email);
            // Deliberately unusable: the owner sets their own through the
            // invitation, and nobody - including this admin - ever knows this one.
            owner.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            owner.setFullName(request.ownerFullName() == null || request.ownerFullName().isBlank()
                    ? null : request.ownerFullName().trim());
            owner.setRole(Role.BUSINESS_OWNER);
            owner.setStatus(AccountStatus.PENDING_VERIFICATION);
            accountRepository.save(owner);
        } else if (owner.getStatus() == AccountStatus.DELETED) {
            throw new BusinessRuleViolationException(
                    "That account has been deleted. Use a different email address.");
        }

        TemplateType templateType = request.templateType();
        LayoutVariant layoutVariant = request.layoutVariant() != null
                ? request.layoutVariant() : LayoutVariant.defaultFor(templateType);
        if (layoutVariant.templateType() != templateType) {
            throw new BusinessRuleViolationException(
                    "That template does not belong to the chosen kind of website.");
        }

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(owner);
        website.setBusinessName(request.businessName().trim());
        website.setSlug(slugGenerator.generateUniqueSlug(request.businessName().trim()));
        website.setPageMode(request.pageMode() != null ? request.pageMode() : PageMode.ONE_PAGE);
        website.setTemplateType(templateType);
        website.setLayoutVariant(layoutVariant);
        website.setStatus(WebsiteStatus.DRAFT);
        websiteRepository.save(website);

        if (request.complimentary()) {
            Plan plan = planRepository.findByCodeAndBillingPeriod(PlanCode.BASIC, BillingPeriod.MONTHLY)
                    .orElseThrow(() -> new ResourceNotFoundException("No BASIC plan configured to base free access on."));
            Subscription free = new Subscription();
            free.setWebsite(website);
            free.setPlan(plan);
            free.setStatus(SubscriptionStatus.ACTIVE);
            free.setStartDate(Instant.now());
            // No end date and no grace: nothing to expire, nothing to chase.
            free.setEndDate(null);
            free.setGraceEndsAt(null);
            free.setComplimentary(true);
            subscriptionRepository.save(free);
        }

        auditService.record(caller.accountId(), "WEBSITE_PROVISIONED_FOR_OWNER", website.getId().toString());

        if (created) {
            inviteOwnerToSetPassword(owner, website.getBusinessName());
        } else {
            emailService.send(owner.getEmail(), "A website was set up for you",
                    "\"" + website.getBusinessName() + "\" is now on your account. Sign in to finish setting it up.");
        }

        return new ProvisionedWebsiteResponse(website.getId(), website.getBusinessName(), website.getSlug(),
                owner.getId(), owner.getEmail(), created, request.complimentary());
    }

    /**
     * Invites a newly created owner to choose their password.
     *
     * Reuses the password-reset token rather than inventing a second kind: the
     * link does the same job - prove you hold this mailbox, then set a password -
     * and completing it verifies the address too, so there is nothing else for
     * the owner to do before signing in.
     */
    private void inviteOwnerToSetPassword(Account owner, String businessName) {
        AccountToken token = new AccountToken();
        token.setAccount(owner);
        token.setType(TokenType.PASSWORD_RESET);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(businessRules.getEmailVerificationTokenTtlHours(), ChronoUnit.HOURS));
        tokenRepository.save(token);

        emailService.send(owner.getEmail(), "Your website is ready",
                "A website has been set up for you: \"" + businessName + "\". "
                        + "Choose a password to sign in: " + frontendBaseUrl + "/reset-password?token=" + token.getToken());
    }

    /** Every template's price, for the pricing screen. */
    @Transactional(readOnly = true)
    public List<TemplatePriceResponse> listTemplatePrices(AuthenticatedAccount caller) {
        requireSuperAdmin(caller);
        return templatePriceRepository.findAllByOrderByLayoutVariantAsc().stream()
                .map(TemplatePriceResponse::from)
                .toList();
    }

    /**
     * Reprices one template.
     *
     * Existing subscriptions are untouched on purpose: somebody who has already
     * paid keeps what they paid for until it ends, and the new price applies at
     * their next checkout. Repricing is not a way to charge people retroactively.
     */
    @Transactional
    public TemplatePriceResponse updateTemplatePrice(
            AuthenticatedAccount caller, LayoutVariant layoutVariant, TemplatePriceUpdateRequest request) {
        requireSuperAdmin(caller);

        if (request.yearlyPrice().compareTo(request.monthlyPrice()) < 0) {
            throw new BusinessRuleViolationException(
                    "The yearly price is lower than the monthly one. Check the figures before saving.");
        }
        if (planRepository.findByCodeAndBillingPeriod(request.planCode(), BillingPeriod.MONTHLY).isEmpty()) {
            throw new ResourceNotFoundException("That plan does not exist, so its limits cannot be granted.");
        }

        TemplatePrice price = templatePriceRepository.findByLayoutVariant(layoutVariant)
                .orElseThrow(() -> new ResourceNotFoundException("That template has no price row."));
        price.setMonthlyPrice(request.monthlyPrice());
        price.setYearlyPrice(request.yearlyPrice());
        price.setPlanCode(request.planCode());
        price.setActive(request.active());

        auditService.record(caller.accountId(), "TEMPLATE_PRICE_UPDATED", layoutVariant.name());
        return TemplatePriceResponse.from(price);
    }

    /** How many days of history the platform report covers by default. */
    private static final int REPORT_DAYS = 30;
    /** Guard: a caller-supplied window is clamped so one request cannot ask for years of series. */
    private static final int MAX_REPORT_DAYS = 365;

    /**
     * The platform report: what is happening, not just how big things are.
     *
     * Counted in memory from rows that already exist rather than through a
     * dozen aggregate queries. At this scale that is the simpler, more readable
     * choice, and it keeps every figure derived from the same snapshot - a
     * report whose halves disagree because they were queried seconds apart is
     * worse than one that takes a moment longer.
     */
    @Transactional(readOnly = true)
    public AdminPlatformReportResponse getPlatformReport(AuthenticatedAccount caller, Integer requestedDays) {
        requireSuperAdmin(caller);
        int days = Math.min(Math.max(requestedDays == null ? REPORT_DAYS : requestedDays, 1), MAX_REPORT_DAYS);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = today.minusDays(days - 1L);

        List<BusinessWebsite> websites = websiteRepository.findAll();
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        List<MockPayment> payments = mockPaymentRepository.findAll();

        // --- which templates people actually choose ---
        Map<LayoutVariant, List<BusinessWebsite>> byVariant = websites.stream()
                .filter(w -> w.getEffectiveLayoutVariant() != null)
                .collect(Collectors.groupingBy(BusinessWebsite::getEffectiveLayoutVariant));
        List<AdminPlatformReportResponse.TemplateUsage> templates = Arrays.stream(LayoutVariant.values())
                .map(variant -> {
                    List<BusinessWebsite> group = byVariant.getOrDefault(variant, List.of());
                    long published = group.stream().filter(w -> w.getPublishedAt() != null).count();
                    return new AdminPlatformReportResponse.TemplateUsage(
                            variant.name(), variant.templateType().name(), group.size(), published);
                })
                .toList();

        // --- daily series, gap-filled so a quiet day is a zero rather than a hole ---
        List<AdminPlatformReportResponse.DailyCount> signups = countByDay(
                accountRepository.findAll().stream().map(a -> dayOf(a.getCreatedAt())), from, today);
        List<AdminPlatformReportResponse.DailyCount> created = countByDay(
                websites.stream().map(w -> dayOf(w.getCreatedAt())), from, today);
        List<AdminPlatformReportResponse.DailyCount> published = countByDay(
                websites.stream().filter(w -> w.getPublishedAt() != null).map(w -> dayOf(w.getPublishedAt())), from, today);

        List<MockPayment> paid = payments.stream()
                .filter(p -> p.getStatus() == MockPaymentStatus.SUCCESS)
                .toList();
        Map<LocalDate, BigDecimal> takings = paid.stream().collect(Collectors.groupingBy(
                p -> dayOf(p.getCreatedAt()),
                Collectors.reducing(BigDecimal.ZERO, MockPayment::getAmount, BigDecimal::add)));
        List<AdminPlatformReportResponse.DailyAmount> revenue = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            revenue.add(new AdminPlatformReportResponse.DailyAmount(d, takings.getOrDefault(d, BigDecimal.ZERO)));
        }

        // --- the subscription mix as it stands ---
        Map<SubscriptionStatus, Long> statusCounts = subscriptions.stream()
                .collect(Collectors.groupingBy(Subscription::getStatus, Collectors.counting()));
        List<AdminPlatformReportResponse.StatusCount> byStatus = Arrays.stream(SubscriptionStatus.values())
                .map(status -> new AdminPlatformReportResponse.StatusCount(
                        status.name(), statusCounts.getOrDefault(status, 0L)))
                .toList();

        // --- where the money comes from ---
        Map<String, List<MockPayment>> byPlan = paid.stream()
                .filter(p -> p.getSubscription() != null && p.getSubscription().getPlan() != null)
                .collect(Collectors.groupingBy(p -> p.getSubscription().getPlan().getCode().name()
                        + "|" + p.getSubscription().getPlan().getBillingPeriod().name()));
        List<AdminPlatformReportResponse.PlanRevenue> revenueByPlan = byPlan.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    BigDecimal total = entry.getValue().stream()
                            .map(MockPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AdminPlatformReportResponse.PlanRevenue(
                            parts[0], parts[1], entry.getValue().size(), total);
                })
                .sorted(Comparator.comparing(AdminPlatformReportResponse.PlanRevenue::revenue).reversed())
                .toList();

        // --- a first payment, or a renewal ---
        // The first successful payment on a subscription is somebody starting;
        // every later one is somebody choosing to stay, which is the number that
        // actually says whether this is working.
        long renewals = 0;
        long firstPayments = 0;
        Map<UUID, List<MockPayment>> paidBySubscription = paid.stream()
                .filter(p -> p.getSubscription() != null)
                .collect(Collectors.groupingBy(p -> p.getSubscription().getId()));
        for (List<MockPayment> forOne : paidBySubscription.values()) {
            firstPayments += 1;
            renewals += Math.max(0, forOne.size() - 1);
        }

        long onFreeTrial = statusCounts.getOrDefault(SubscriptionStatus.TRIAL, 0L);
        // A trial that ended and was never paid for: expired, never had a grace
        // period (only paid plans get one), and no successful payment behind it.
        long trialsLapsed = subscriptions.stream()
                .filter(sub -> sub.getStatus() == SubscriptionStatus.EXPIRED)
                .filter(sub -> sub.getGraceEndsAt() == null)
                .filter(sub -> !paidBySubscription.containsKey(sub.getId()))
                .count();

        return new AdminPlatformReportResponse(days, templates, signups, created, published, revenue,
                byStatus, revenueByPlan, firstPayments, renewals, onFreeTrial, trialsLapsed);
    }

    private static LocalDate dayOf(Instant instant) {
        return instant == null ? LocalDate.EPOCH : instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    /** A gap-filled daily count, so the series has one point per day whether anything happened or not. */
    private static List<AdminPlatformReportResponse.DailyCount> countByDay(
            java.util.stream.Stream<LocalDate> dates, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> counts = dates.collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        List<AdminPlatformReportResponse.DailyCount> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            out.add(new AdminPlatformReportResponse.DailyCount(d, counts.getOrDefault(d, 0L)));
        }
        return out;
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
