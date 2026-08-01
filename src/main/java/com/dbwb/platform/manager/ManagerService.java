package com.dbwb.platform.manager;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.dto.InviteManagerRequest;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.notification.NotificationService;
import com.dbwb.platform.notification.entity.NotificationEvent;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Section 8.3 + 9.14: invite-by-email flow with granular, individually
 * assigned permissions (BR-MGR-003), plan-enforced Manager limits
 * (BR-MGR-006), and non-destructive removal (BR-MGR-005).
 *
 * Phase 4: the invited person must always explicitly accept/reject
 * (BR-MGR-008) - an invitation is never auto-accepted just because an
 * account with the same email already exists.
 */
@Service
public class ManagerService {

    /** Statuses that count as "already has an active claim" on this website, for duplicate-invite and plan-limit checks. */
    private static final Set<InvitationStatus> ACTIVE_STATUSES = EnumSet.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED);

    private final ManagerAccessRepository managerAccessRepository;
    private final AccountRepository accountRepository;
    private final WebsiteAccessGuard accessGuard;
    private final SubscriptionQueryService subscriptionQueryService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final BusinessRuleProperties businessRules;

    public ManagerService(
            ManagerAccessRepository managerAccessRepository,
            AccountRepository accountRepository,
            WebsiteAccessGuard accessGuard,
            SubscriptionQueryService subscriptionQueryService,
            EmailService emailService,
            NotificationService notificationService,
            AuditService auditService,
            BusinessRuleProperties businessRules) {
        this.managerAccessRepository = managerAccessRepository;
        this.accountRepository = accountRepository;
        this.accessGuard = accessGuard;
        this.subscriptionQueryService = subscriptionQueryService;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.businessRules = businessRules;
    }

    @Transactional
    public ManagerAccess invite(UUID websiteId, AuthenticatedAccount caller, InviteManagerRequest request) {
        BusinessWebsite website = accessGuard.requireOwner(websiteId, caller);

        if (website.getOwner().getEmail().equalsIgnoreCase(request.email())) {
            throw new BusinessRuleViolationException("You cannot invite yourself - you already own this website.");
        }
        if (managerAccessRepository.existsByWebsiteIdAndInvitedEmailIgnoreCaseAndStatusIn(websiteId, request.email(), ACTIVE_STATUSES)) {
            throw new BusinessRuleViolationException("This person already has a pending invitation or access to this website.");
        }

        // BR-MGR-006: plan-enforced manager cap. A pending invite already counts toward it, so an
        // owner can't dodge the limit by sending more invites than seats before any are accepted.
        Plan activePlan = subscriptionQueryService.getActivePlan(websiteId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "An active subscription is required before inviting managers."));
        long currentManagers = managerAccessRepository.countByWebsiteIdAndStatusIn(websiteId, ACTIVE_STATUSES);
        if (currentManagers >= activePlan.getMaxManagersPerWebsite()) {
            throw new BusinessRuleViolationException(
                    "This website's plan allows up to " + activePlan.getMaxManagersPerWebsite() + " manager(s). Upgrade your plan to invite more.");
        }

        ManagerAccess access = new ManagerAccess();
        access.setWebsite(website);
        access.setInvitedEmail(request.email());
        access.setPermissions(request.permissions());
        access.setStatus(InvitationStatus.PENDING);

        // BR-MGR-002: if the invitee already has an account, link it now so the invitation shows up
        // for them - but it stays PENDING until they explicitly accept (BR-MGR-008).
        Account existingAccount = accountRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (existingAccount != null) {
            access.setManagerAccount(existingAccount);
        }

        managerAccessRepository.save(access);

        if (existingAccount != null) {
            notificationService.notify(existingAccount.getId(), NotificationEvent.MANAGER_INVITATION,
                    "You've been invited to help manage \"" + website.getBusinessName() + "\".");
        }
        emailService.send(request.email(), "You've been invited to manage a website",
                "You have been invited to help manage \"" + website.getBusinessName() + "\" on the platform. "
                        + (existingAccount != null
                        ? "Log in and check your invitations to accept or decline."
                        : "Register with this email address, then accept or decline the invitation from your dashboard."));

        auditService.record(caller.accountId(), "MANAGER_INVITED", access.getId().toString());
        return access;
    }

    /** Called right after a new Account is created, to link (not auto-accept) any invitations sent before it existed (BR-MGR-002/008). */
    @Transactional
    public void linkPendingInvitationsForNewAccount(Account account) {
        managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus(account.getEmail(), InvitationStatus.PENDING)
                .forEach(access -> {
                    access.setManagerAccount(account);
                    notificationService.notify(account.getId(), NotificationEvent.MANAGER_INVITATION,
                            "You've been invited to help manage \"" + access.getWebsite().getBusinessName() + "\".");
                });
    }

    @Transactional(readOnly = true)
    public List<ManagerAccess> listForWebsite(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireOwner(websiteId, caller);
        return managerAccessRepository.findByWebsiteId(websiteId);
    }

    /** The current account's own PENDING invitations, across every website - what they need to act on. */
    @Transactional(readOnly = true)
    public List<ManagerAccess> listMyInvitations(AuthenticatedAccount caller) {
        return managerAccessRepository.findByManagerAccountIdAndStatus(caller.accountId(), InvitationStatus.PENDING);
    }

    /** BR-MGR-008: the invited person explicitly accepts - never automatic. */
    @Transactional
    public ManagerAccess accept(UUID accessId, AuthenticatedAccount caller) {
        ManagerAccess access = loadOwnPendingInvitation(accessId, caller);
        access.setStatus(InvitationStatus.ACCEPTED);
        auditService.record(caller.accountId(), "MANAGER_INVITATION_ACCEPTED", accessId.toString());
        return access;
    }

    /** BR-MGR-008: the invited person explicitly declines. */
    @Transactional
    public void reject(UUID accessId, AuthenticatedAccount caller) {
        ManagerAccess access = loadOwnPendingInvitation(accessId, caller);
        access.setStatus(InvitationStatus.REJECTED);
        auditService.record(caller.accountId(), "MANAGER_INVITATION_REJECTED", accessId.toString());
    }

    @Transactional
    public void updatePermissions(UUID websiteId, UUID accessId, AuthenticatedAccount caller, Set<Permission> permissions) {
        accessGuard.requireOwner(websiteId, caller);
        ManagerAccess access = load(accessId, websiteId);
        access.setPermissions(permissions);
    }

    /** BR-MGR-005: revokes only this website's access (soft-delete, so the record stays as history and the email can be re-invited); the Manager's Account is never touched. */
    @Transactional
    public void revoke(UUID websiteId, UUID accessId, AuthenticatedAccount caller) {
        accessGuard.requireOwner(websiteId, caller);
        ManagerAccess access = load(accessId, websiteId);
        access.setStatus(InvitationStatus.REVOKED);
        auditService.record(caller.accountId(), "MANAGER_ACCESS_REVOKED", accessId.toString());
    }

    /** BR-MGR-007: invitations left PENDING too long auto-expire, so a stale invite can't be accepted years later. */
    @Transactional
    public void runExpiryMaintenance() {
        Instant cutoff = Instant.now().minus(businessRules.getManagerInvitationExpiryDays(), ChronoUnit.DAYS);
        managerAccessRepository.findByStatusAndCreatedAtBefore(InvitationStatus.PENDING, cutoff)
                .forEach(access -> access.setStatus(InvitationStatus.EXPIRED));
    }

    private ManagerAccess loadOwnPendingInvitation(UUID accessId, AuthenticatedAccount caller) {
        ManagerAccess access = managerAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        if (access.getManagerAccount() == null || !access.getManagerAccount().getId().equals(caller.accountId())) {
            throw new AccessDeniedForTenantException("This invitation does not belong to you.");
        }
        if (access.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleViolationException("This invitation is no longer pending.");
        }
        return access;
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
