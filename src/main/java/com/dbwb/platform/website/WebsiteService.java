package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.audit.AuditService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
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
    private final AuditService auditService;
    private final PublicWebsiteService publicWebsiteService;
    private final ManagerAccessRepository managerAccessRepository;

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
            AuditService auditService,
            PublicWebsiteService publicWebsiteService,
            ManagerAccessRepository managerAccessRepository) {
        this.websiteRepository = websiteRepository;
        this.themeRepository = themeRepository;
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.categoryRepository = categoryRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.slugGenerator = slugGenerator;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
        this.auditService = auditService;
        this.publicWebsiteService = publicWebsiteService;
        this.managerAccessRepository = managerAccessRepository;
    }

    /** Phase 4: a website plus the caller's role/permissions on it, so the frontend can gate UI without re-deriving access logic. */
    public record WebsiteAccessInfo(BusinessWebsite website, AccessRole role, Set<Permission> permissions) {
    }

    @Transactional
    public BusinessWebsite create(AuthenticatedAccount caller, CreateWebsiteRequest request) {
        // NOTE: "number of websites per Business Owner determined by plan" (7.2) is
        // listed as TBD-003 in the BRD (exact feature/limit matrix not finalized).
        // Enforce here once that matrix is approved - intentionally not hardcoded.

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
        website.setOrderingMode(request.orderingMode());
        return website;
    }

    /**
     * BR-THEME-006 / BR-RULE-001/013: validates all mandatory publication
     * fields and an active subscription/grace period before allowing Publish.
     * On success, promotes draft -> published and retains exactly one prior
     * published version (BR-THEME-007).
     */
    @Transactional
    public BusinessWebsite publish(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.PUBLISH_WEBSITE);

        if (!subscriptionQueryService.hasPublishableSubscription(websiteId)) {
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

    /** Layout is a structural choice, not just styling - switchable anytime, same as Theme, but scoped to the website's current TemplateType. */
    @Transactional
    public BusinessWebsite updateLayoutVariant(UUID websiteId, AuthenticatedAccount caller, com.dbwb.platform.website.entity.LayoutVariant layoutVariant) {
        BusinessWebsite website = accessGuard.requirePermission(
                websiteId, caller, com.dbwb.platform.manager.entity.Permission.MANAGE_THEME_AND_CONTENT);
        if (layoutVariant.templateType() != website.getTemplateType()) {
            throw new BusinessRuleViolationException(
                    "\"" + layoutVariant + "\" is not a valid layout for a " + website.getTemplateType() + " website.");
        }
        website.setLayoutVariant(layoutVariant);
        return website;
    }

    /** BR-THEME-007: restore the immediately previous published version. */
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

        if (website.getTemplateType() == TemplateType.PORTFOLIO) {
            if (serviceItemRepository.countByWebsiteId(website.getId()) == 0) {
                throw new BusinessRuleViolationException("At least one service is required before publishing.");
            }
        } else {
            long categoryCount = categoryRepository.countByWebsiteId(website.getId());
            if (categoryCount == 0) {
                throw new BusinessRuleViolationException("At least one menu category and item is required before publishing.");
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
