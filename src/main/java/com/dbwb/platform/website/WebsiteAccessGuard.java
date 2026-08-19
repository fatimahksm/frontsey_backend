package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * BR-RULE-003: every protected business action must be authorized against
 * website ownership or an assigned Manager permission. Every module that
 * reads/writes tenant data (menu, profile, delivery, manager, subscription...)
 * should route through this single guard instead of re-implementing the
 * ownership check, so the tenant-isolation rule cannot drift between modules.
 */
@Component
public class WebsiteAccessGuard {

    /**
     * The actions that change the website, as opposed to looking at it.
     *
     * A website whose subscription has stopped is frozen, not deleted: the
     * owner can still sign in, open every page and read everything, and can of
     * course pay - but nothing that alters the site goes through. Without this
     * the plan ending only unpublished the public page, and the owner carried
     * on adding menu items and editing content on a site nobody could reach.
     *
     * VIEW_ANALYTICS is deliberately absent. It is the one permission that only
     * reads, and reading your own numbers is not control over the site.
     */
    private static final Set<Permission> WRITE_PERMISSIONS = EnumSet.of(
            Permission.MANAGE_MENU,
            Permission.MANAGE_PRICES,
            Permission.MANAGE_THEME_AND_CONTENT,
            Permission.PUBLISH_WEBSITE,
            Permission.MANAGE_BUSINESS_PROFILE,
            Permission.MANAGE_DELIVERY_SETTINGS);

    private final BusinessWebsiteRepository websiteRepository;
    private final ManagerAccessRepository managerAccessRepository;
    private final SubscriptionRepository subscriptionRepository;

    public WebsiteAccessGuard(BusinessWebsiteRepository websiteRepository,
                               ManagerAccessRepository managerAccessRepository,
                               SubscriptionRepository subscriptionRepository) {
        this.websiteRepository = websiteRepository;
        this.managerAccessRepository = managerAccessRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /** Loads the website and confirms the caller may view it at all (owner, permitted Manager, or Super Admin). */
    public BusinessWebsite requireReadAccess(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = load(websiteId);

        if (caller.role() == Role.SUPER_ADMIN) {
            return website;
        }
        if (website.getOwner().getId().equals(caller.accountId())) {
            return website;
        }
        boolean isAcceptedManager = managerAccessRepository
                .findByWebsiteIdAndManagerAccountId(websiteId, caller.accountId())
                .filter(access -> access.getStatus() == InvitationStatus.ACCEPTED)
                .isPresent();
        if (isAcceptedManager) {
            return website;
        }

        throw new AccessDeniedForTenantException("You do not have access to this website.");
    }

    /**
     * Confirms the caller may perform a specific action: the Owner always can;
     * a Manager only can if explicitly granted that permission (BR-MGR-003/004).
     */
    public BusinessWebsite requirePermission(UUID websiteId, AuthenticatedAccount caller, Permission permission) {
        BusinessWebsite website = load(websiteId);

        if (caller.role() == Role.SUPER_ADMIN) {
            return website;
        }

        requireLiveSubscriptionFor(websiteId, permission);

        if (website.getOwner().getId().equals(caller.accountId())) {
            return website;
        }

        boolean hasPermission = managerAccessRepository
                .findByWebsiteIdAndManagerAccountId(websiteId, caller.accountId())
                .filter(access -> access.getStatus() == InvitationStatus.ACCEPTED)
                .map(access -> access.getPermissions().contains(permission))
                .orElse(false);

        if (!hasPermission) {
            throw new AccessDeniedForTenantException("You do not have permission to perform this action.");
        }
        return website;
    }

    /** Owner-only action (e.g. inviting/removing Managers, changing subscription). */
    public BusinessWebsite requireOwner(UUID websiteId, AuthenticatedAccount caller) {
        BusinessWebsite website = load(websiteId);
        if (caller.role() == Role.SUPER_ADMIN) {
            return website;
        }
        if (!website.getOwner().getId().equals(caller.accountId())) {
            throw new AccessDeniedForTenantException("Only the Business Owner can perform this action.");
        }
        return website;
    }

    /**
     * Refuses anything that would change a website whose subscription has
     * stopped. Checked before ownership on purpose - this applies to the owner
     * as much as to a manager, and an owner who has stopped paying should hit
     * the same wall.
     *
     * A website that has never had a subscription is untouched: it has not
     * published yet, so it has nothing to freeze, and its free trial opens the
     * moment it does. Anything the maintenance job left in EXPIRED after really
     * running - a plan that ran out, or a trial that finished - is frozen until
     * it is paid for.
     */
    private void requireLiveSubscriptionFor(UUID websiteId, Permission permission) {
        if (!WRITE_PERMISSIONS.contains(permission)) {
            return;
        }
        subscriptionRepository.findByWebsiteId(websiteId).ifPresent(subscription -> {
            // A row that never served a day is not a plan that stopped - it is a
            // zero-length trial left by a misconfiguration, and publishing is
            // what repairs it. Freezing here would make that unreachable.
            if (!subscription.hasEverRun()) {
                return;
            }
            boolean live = subscription.getStatus() == SubscriptionStatus.TRIAL
                    || subscription.getStatus() == SubscriptionStatus.ACTIVE
                    || subscription.getStatus() == SubscriptionStatus.GRACE
                    || subscription.getStatus() == SubscriptionStatus.PENDING;
            if (!live) {
                throw new BusinessRuleViolationException(
                        "This website is locked because its plan ended. Subscribe to a plan to edit it again.");
            }
        });
    }

    private BusinessWebsite load(UUID websiteId) {
        return websiteRepository.findById(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found."));
    }
}
