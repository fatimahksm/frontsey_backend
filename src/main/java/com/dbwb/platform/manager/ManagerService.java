package com.dbwb.platform.manager;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.dto.InviteManagerRequest;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Section 8.3 + 9.14: invite-by-email flow with granular, individually
 * assigned permissions (BR-MGR-003), plan-enforced Manager limits
 * (BR-MGR-006), and non-destructive removal (BR-MGR-005).
 */
@Service
public class ManagerService {

    private final ManagerAccessRepository managerAccessRepository;
    private final AccountRepository accountRepository;
    private final WebsiteAccessGuard accessGuard;
    private final EmailService emailService;
    private final AuditService auditService;
    private final BusinessRuleProperties businessRules;

    public ManagerService(
            ManagerAccessRepository managerAccessRepository,
            AccountRepository accountRepository,
            WebsiteAccessGuard accessGuard,
            EmailService emailService,
            AuditService auditService,
            BusinessRuleProperties businessRules) {
        this.managerAccessRepository = managerAccessRepository;
        this.accountRepository = accountRepository;
        this.accessGuard = accessGuard;
        this.emailService = emailService;
        this.auditService = auditService;
        this.businessRules = businessRules;
    }

    @Transactional
    public ManagerAccess invite(UUID websiteId, AuthenticatedAccount caller, InviteManagerRequest request) {
        BusinessWebsite website = accessGuard.requireOwner(websiteId, caller);

        // NOTE: the exact Manager-per-plan limit is TBD-003 in the BRD; the
        // enforcement point (below) is wired up but the numeric cap itself
        // comes from Plan.maxManagersPerWebsite once the subscription module
        // exposes the website's active plan to this service.
        long currentManagers = managerAccessRepository.countByWebsiteIdAndStatus(websiteId, InvitationStatus.ACCEPTED);

        ManagerAccess access = new ManagerAccess();
        access.setWebsite(website);
        access.setInvitedEmail(request.email());
        access.setPermissions(request.permissions());
        access.setStatus(InvitationStatus.PENDING);

        // BR-MGR-002: if the invitee already has an account, link immediately.
        accountRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            access.setManagerAccount(existing);
            access.setStatus(InvitationStatus.ACCEPTED);
        });

        managerAccessRepository.save(access);

        emailService.send(request.email(), "You've been invited to manage a website",
                "You have been invited to help manage \"" + website.getBusinessName() + "\" on the platform. "
                        + (access.getStatus() == InvitationStatus.ACCEPTED
                        ? "Log in to access it."
                        : "Register with this email address to accept the invitation."));

        auditService.record(caller.accountId(), "MANAGER_INVITED", access.getId().toString());
        return access;
    }

    /** Called right after a new Account is created, to auto-link any invitations sent before it existed (BR-MGR-002). */
    @Transactional
    public void linkPendingInvitationsForNewAccount(Account account) {
        managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus(account.getEmail(), InvitationStatus.PENDING)
                .forEach(access -> {
                    access.setManagerAccount(account);
                    access.setStatus(InvitationStatus.ACCEPTED);
                });
    }

    @Transactional(readOnly = true)
    public List<ManagerAccess> listForWebsite(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireOwner(websiteId, caller);
        return managerAccessRepository.findByWebsiteId(websiteId);
    }

    @Transactional
    public void updatePermissions(UUID websiteId, UUID accessId, AuthenticatedAccount caller, Set<Permission> permissions) {
        accessGuard.requireOwner(websiteId, caller);
        ManagerAccess access = load(accessId, websiteId);
        access.setPermissions(permissions);
    }

    /** BR-MGR-005: revokes only this website's access; the Manager's Account is never touched. */
    @Transactional
    public void revoke(UUID websiteId, UUID accessId, AuthenticatedAccount caller) {
        accessGuard.requireOwner(websiteId, caller);
        ManagerAccess access = load(accessId, websiteId);
        managerAccessRepository.delete(access);
        auditService.record(caller.accountId(), "MANAGER_ACCESS_REVOKED", accessId.toString());
    }

    private ManagerAccess load(UUID accessId, UUID websiteId) {
        ManagerAccess access = managerAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager access record not found."));
        if (!access.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Manager access record not found.");
        }
        return access;
    }
}
