package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.CacheConfig;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.publicapi.PublicWebsiteService;
import com.dbwb.platform.publicapi.dto.PublicWebsiteResponse;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.subscription.SubscriptionService;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.theme.entity.Theme;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.website.dto.AccessRole;
import com.dbwb.platform.website.dto.CreateWebsiteRequest;
import com.dbwb.platform.website.dto.UpdateDraftContentRequest;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.account.repository.AccountRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import com.dbwb.platform.plan.entity.Plan;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the Business Owner Onboarding journey (Section 8.1) end to end
 * except payment itself, which lives in the subscription module.
 */
@Service
public class WebsiteService {

    private final BusinessWebsiteRepository websiteRepository;
    private final ThemeRepository themeRepository;
    private final AccountRepository accountRepository;
    private final BusinessProfileRepository profileRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final SlugGenerator slugGenerator;
    private final WebsiteAccessGuard accessGuard;
    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscriptionService subscriptionService;
    private final AuditService auditService;
    private final PublicWebsiteService publicWebsiteService;
    private final ManagerAccessRepository managerAccessRepository;
    private final ThemeConfigValidator themeConfigValidator;
    private final com.dbwb.platform.plan.TemplateAvailability templateAvailability;
    private final com.dbwb.platform.common.config.BusinessRuleProperties businessRules;

    public WebsiteService(
            BusinessWebsiteRepository websiteRepository,
            ThemeRepository themeRepository,
            AccountRepository accountRepository,
            BusinessProfileRepository profileRepository,
            CategoryRepository categoryRepository,
            ServiceItemRepository serviceItemRepository,
            SlugGenerator slugGenerator,
            WebsiteAccessGuard accessGuard,
            SubscriptionQueryService subscriptionQueryService,
            SubscriptionService subscriptionService,
            AuditService auditService,
            PublicWebsiteService publicWebsiteService,
            ManagerAccessRepository managerAccessRepository,
            ThemeConfigValidator themeConfigValidator,
            com.dbwb.platform.plan.TemplateAvailability templateAvailability,
            com.dbwb.platform.common.config.BusinessRuleProperties businessRules) {
        this.websiteRepository = websiteRepository;
        this.themeRepository = themeRepository;
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.slugGenerator = slugGenerator;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
        this.subscriptionService = subscriptionService;
        this.auditService = auditService;
        this.publicWebsiteService = publicWebsiteService;
        this.themeConfigValidator = themeConfigValidator;
        this.managerAccessRepository = managerAccessRepository;
        this.templateAvailability = templateAvailability;
        this.businessRules = businessRules;
    }

    /** Phase 4: a website plus the caller's role/permissions on it, so the frontend can gate UI without re-deriving access logic. */
    public record WebsiteAccessInfo(BusinessWebsite website, AccessRole role, Set<Permission> permissions) {
    }

    @Transactional
    public BusinessWebsite create(AuthenticatedAccount caller, CreateWebsiteRequest request) {
        requireRoomForAnotherWebsite(caller);

        // Nothing of this kind on offer means the layout would fall back to
        // LayoutVariant.defaultFor(), which may itself be a withdrawn template -
        // so the website would be created straight onto something the owner was
        // never allowed to pick.
        templateAvailability.requireAnyOffered(request.templateType());

        BusinessWebsite website = new BusinessWebsite();
        website.setOwner(accountRepository.getReferenceById(caller.accountId()));
        website.setBusinessName(request.businessName());
        website.setSlug(slugGenerator.generateUniqueSlug(request.businessName()));
        website.setPageMode(request.pageMode());
        website.setTemplateType(request.templateType());
        website.setStatus(WebsiteStatus.DRAFT);

        if (request.themeId() != null) {
            Theme theme = themeRepository.findById(request.themeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
            website.setTheme(theme);
        }
        // else: build-from-scratch (BR-SITE-002) - theme left null, frontend uses
        // predefined sections directly instead of a preset theme.

        websiteRepository.save(website);
        auditService.record(caller.accountId(), "WEBSITE_CREATED", website.getId().toString());
        return website;
    }

    @Transactional(readOnly = true)
    public List<BusinessWebsite> listForOwner(UUID ownerId) {
        return websiteRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public BusinessWebsite get(UUID websiteId, AuthenticatedAccount caller) {
        return accessGuard.requireReadAccess(websiteId, caller);
    }

    /** Same access check as get(), but also resolves the caller's role/permissions for this one website. */
    @Transactional(readOnly = true)
    public WebsiteAccessInfo getWithAccess(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = accessGuard.requireReadAccess(websiteId, caller);
        return accessInfoFor(website, caller);
    }

    /** Phase 4 (BR-MGR): every website the caller owns, plus every website they have ACCEPTED manager access to. */
    @Transactional(readOnly = true)
    public List<WebsiteAccessInfo> listAccessible(AuthenticatedAccount caller) {
        List<WebsiteAccessInfo> result = new ArrayList<>();
        websiteRepository.findByOwnerId(caller.accountId())
                .forEach(website -> result.add(new WebsiteAccessInfo(website, AccessRole.OWNER, Set.of())));
        managerAccessRepository.findByManagerAccountIdAndStatus(caller.accountId(), InvitationStatus.ACCEPTED)
                .forEach(access -> result.add(new WebsiteAccessInfo(access.getWebsite(), AccessRole.MANAGER, access.getPermissions())));
        return result;
    }

    private WebsiteAccessInfo accessInfoFor(BusinessWebsite website, AuthenticatedAccount caller) {
        if (caller.role() == Role.SUPER_ADMIN || website.getOwner().getId().equals(caller.accountId())) {
            return new WebsiteAccessInfo(website, AccessRole.OWNER, Set.of());
        }
        Set<Permission> permissions = managerAccessRepository
                .findByWebsiteIdAndManagerAccountId(website.getId(), caller.accountId())
                .map(ManagerAccess::getPermissions)
                .orElse(Set.of());
        return new WebsiteAccessInfo(website, AccessRole.MANAGER, permissions);
    }

    /** Lets the owner/a manager see the current draft rendered exactly as customers would see it, before publishing. */
    @Transactional(readOnly = true)
    public PublicWebsiteResponse getPreview(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = accessGuard.requireReadAccess(websiteId, caller);
        return publicWebsiteService.assembleForPreview(website);
    }

    @Transactional
    public BusinessWebsite saveDraft(UUID websiteId, AuthenticatedAccount caller, UpdateDraftContentRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);
        website.setDraftContent(request.content());
        // A display-only layout has no cart to order from, so it pins the
        // ordering mode regardless of what the client sends.
        website.setOrderingMode(website.getEffectiveLayoutVariant().isDisplayOnly()
                ? com.dbwb.platform.website.entity.OrderingMode.DISPLAY_ONLY
                : request.orderingMode());
        return website;
    }

    /**
     * BR-THEME-006 / BR-RULE-001/013: validates all mandatory publication
     * fields and an active subscription/grace period before allowing Publish.
     * On success, promotes draft -> published and retains exactly one prior
     * published version (BR-THEME-007).
     */
    /**
     * Evicts the public page, on top of the few seconds the cache would have
     * expired in anyway. Publishing is the one change an owner consciously
     * waits on - they press it and go and look - so it is worth being exact
     * about, where an edit to a price can ride the expiry.
     *
     * allEntries because the key is the slug and this method has the id; the
     * cache is small and this happens rarely, so clearing it is cheaper than
     * carrying a slug around to be precise about.
     */
    @CacheEvict(value = CacheConfig.PUBLIC_WEBSITES, allEntries = true)
    @Transactional
    public BusinessWebsite publish(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.PUBLISH_WEBSITE);

        // A website nobody has ever subscribed for gets its free trial here rather
        // than being turned away. Publishing is the moment the link becomes real,
        // so it is the moment worth trialling - and it means creating a website
        // never puts a payment screen in front of somebody who only wants to look.
        if (!subscriptionQueryService.hasPublishableSubscription(websiteId)
                && subscriptionService.startTrialIfEligible(website).isEmpty()) {
            throw new BusinessRuleViolationException(
                    "This website cannot be published without an active subscription or valid grace period.");
        }

        validateMandatoryPublicationFields(website);

        if (website.getPublishedContent() != null) {
            website.setPreviousPublishedContent(website.getPublishedContent()); // BR-THEME-007: one-version rollback
        }
        website.setPublishedContent(website.getDraftContent());
        website.setPublishedAt(Instant.now());
        website.setStatus(WebsiteStatus.PUBLISHED);

        auditService.record(caller.accountId(), "WEBSITE_PUBLISHED", website.getId().toString());
        return website;
    }

    /** BR-SITE-002: switch themes post-creation, or clear it (themeId = null) to go back to build-from-scratch. */
    @Transactional
    public BusinessWebsite updateTheme(UUID websiteId, AuthenticatedAccount caller, UUID themeId) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);
        if (themeId == null) {
            website.setTheme(null);
            return website;
        }
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
        website.setTheme(theme);
        return website;
    }

    /**
     * Replaces this website's own theme overrides, or clears them with null so
     * it goes back to inheriting its preset.
     *
     * Validated up front for the same reason the admin path is: the column is
     * free-text TEXT, and the public renderer reads it on every page load, so
     * anything that would not parse into a ThemeConfig must be rejected here
     * rather than silently falling back at render time.
     */
    @Transactional
    public BusinessWebsite updateThemeConfig(UUID websiteId, AuthenticatedAccount caller, String themeConfig) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);
        if (themeConfig == null || themeConfig.isBlank()) {
            website.setThemeConfig(null);
            return website;
        }
        themeConfigValidator.parseAndValidate(themeConfig);
        website.setThemeConfig(themeConfig);
        return website;
    }

    /** Layout is a structural choice, not just styling - switchable anytime, same as Theme, but scoped to the website's current TemplateType. */
    @Transactional
    public BusinessWebsite updateLayoutVariant(UUID websiteId, AuthenticatedAccount caller, com.dbwb.platform.website.entity.LayoutVariant layoutVariant) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);
        if (layoutVariant.templateType() != website.getTemplateType()) {
            throw new BusinessRuleViolationException(
                    "\"" + layoutVariant + "\" is not a valid layout for a " + website.getTemplateType() + " website.");
        }
        // Re-selecting the template the website is already on is always allowed:
        // withdrawing a template must not trap its existing sites, and this call
        // would then be the only way they could never save this screen again.
        if (layoutVariant != website.getEffectiveLayoutVariant()) {
            templateAvailability.requireOffered(layoutVariant);
        }
        website.setLayoutVariant(layoutVariant);
        // Switching to a cart-less layout is itself the "this is a read-only
        // menu" decision - the owner never has to also find an ordering switch.
        if (layoutVariant.isDisplayOnly()) {
            website.setOrderingMode(com.dbwb.platform.website.entity.OrderingMode.DISPLAY_ONLY);
        }
        return website;
    }

    /** BR-THEME-007: restore the immediately previous published version. */
    /** Same reasoning as publish: an owner rolling back is watching for it. */
    @CacheEvict(value = CacheConfig.PUBLIC_WEBSITES, allEntries = true)
    @Transactional
    public BusinessWebsite restorePreviousVersion(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);

        if (website.getPreviousPublishedContent() == null) {
            throw new BusinessRuleViolationException("No previous published version is available to restore.");
        }
        String current = website.getPublishedContent();
        website.setPublishedContent(website.getPreviousPublishedContent());
        website.setPreviousPublishedContent(current);
        return website;
    }

    /**
     * BRD 7.2: how many websites one owner may have.
     *
     * Left unenforced until now - the note here said to add it once the plan
     * matrix (TBD-003) was approved, and it never was, so creation was
     * unlimited for anyone with an account. Rather than wait longer for a
     * decision that has not come, the number is configuration: an owner with no
     * active plan gets dbwb.business-rules.default-websites-per-owner, and an
     * owner who holds plans gets the largest maxWebsites among them, so the
     * matrix takes over the moment it exists. Setting it to zero restores the
     * old unlimited behaviour.
     *
     * Trashed and deleted websites do not count against it - the limit is on
     * what an owner is running, not on what they have ever made.
     */
    private void requireRoomForAnotherWebsite(AuthenticatedAccount caller) {
        List<BusinessWebsite> live = websiteRepository.findByOwnerId(caller.accountId()).stream()
                .filter(website -> website.getStatus() != WebsiteStatus.DELETED
                        && website.getStatus() != WebsiteStatus.TRASHED)
                .toList();

        int allowance = live.stream()
                .map(website -> subscriptionQueryService.getActivePlan(website.getId()))
                .flatMap(Optional::stream)
                .mapToInt(Plan::getMaxWebsites)
                .max()
                .orElse(businessRules.getDefaultWebsitesPerOwner());

        if (allowance <= 0) {
            return;
        }
        if (live.size() >= allowance) {
            throw new BusinessRuleViolationException(
                    "Your plan covers " + allowance + " website" + (allowance == 1 ? "" : "s")
                            + ". Upgrade, or delete one you are no longer using, to add another.");
        }
    }

    private void validateMandatoryPublicationFields(BusinessWebsite website) {
        // BR-THEME-006: business name, required contact/ordering data, at least
        // one menu category+item, and complete theme configuration as applicable.
        if (website.getBusinessName() == null || website.getBusinessName().isBlank()) {
            throw new BusinessRuleViolationException("Business name is required before publishing.");
        }

        boolean hasProfile = profileRepository.findByWebsiteId(website.getId()).isPresent();
        if (!hasProfile) {
            throw new BusinessRuleViolationException("Business profile/contact information is required before publishing.");
        }

        // A switch rather than "portfolio or else menu": that else meant any new
        // template type silently inherited the menu's rule and demanded a menu
        // category before it could publish.
        switch (website.getTemplateType()) {
            case PORTFOLIO -> {
                if (serviceItemRepository.countByWebsiteId(website.getId()) == 0) {
                    throw new BusinessRuleViolationException("At least one service is required before publishing.");
                }
            }
            case EVENTS -> {
                // Nothing beyond the name and contact details checked above. An
                // invitation with only a date and a photograph is complete; the
                // running order and the gallery are both genuinely optional.
            }
            case MENU_ORDERING -> {
                if (categoryRepository.countByWebsiteId(website.getId()) == 0) {
                    throw new BusinessRuleViolationException("At least one menu category and item is required before publishing.");
                }
            }
        }

        // BR-RULE-013: WhatsApp number mandatory when WhatsApp ordering is enabled.
        if (website.getOrderingMode() == com.dbwb.platform.website.entity.OrderingMode.WHATSAPP_ORDERING) {
            var profile = profileRepository.findByWebsiteId(website.getId()).orElseThrow();
            if (profile.getWhatsappNumber() == null || profile.getWhatsappNumber().isBlank()) {
                throw new BusinessRuleViolationException(
                        "A WhatsApp number is required before publishing when WhatsApp ordering is enabled.");
            }
        }
    }
}
