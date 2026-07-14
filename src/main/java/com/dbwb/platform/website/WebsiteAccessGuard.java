package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.springframework.stereotype.Component;

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

    private final BusinessWebsiteRepository websiteRepository;
    private final ManagerAccessRepository managerAccessRepository;

    public WebsiteAccessGuard(BusinessWebsiteRepository websiteRepository,
                               ManagerAccessRepository managerAccessRepository) {
        this.websiteRepository = websiteRepository;
        this.managerAccessRepository = managerAccessRepository;
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

    private BusinessWebsite load(UUID websiteId) {
        return websiteRepository.findById(websiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Website not found."));
    }
}
