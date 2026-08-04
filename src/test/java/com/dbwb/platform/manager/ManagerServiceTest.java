package com.dbwb.platform.manager;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.dto.InviteManagerRequest;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.notification.NotificationService;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock private ManagerAccessRepository managerAccessRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private WebsiteAccessGuard accessGuard;
    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private BusinessRuleProperties businessRules;

    private ManagerService managerService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount owner = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;
    private Plan plan;

    @BeforeEach
    void setUp() {
        managerService = new ManagerService(managerAccessRepository, accountRepository, accessGuard,
                subscriptionQueryService, emailService, notificationService, auditService, businessRules);

        Account ownerAccount = TestEntities.withId(new Account(), owner.accountId());
        ownerAccount.setEmail("owner@example.com");

        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        website.setBusinessName("Test Business");
        website.setOwner(ownerAccount);

        plan = TestEntities.withId(new Plan(), UUID.randomUUID());
        plan.setMaxManagersPerWebsite(2);

        lenient().when(accessGuard.requireOwner(websiteId, owner)).thenReturn(website);
        lenient().when(subscriptionQueryService.getActivePlan(websiteId)).thenReturn(Optional.of(plan));
        lenient().when(managerAccessRepository.existsByWebsiteIdAndInvitedEmailIgnoreCaseAndStatusIn(any(), any(), any()))
                .thenReturn(false);
        lenient().when(managerAccessRepository.countByWebsiteIdAndStatusIn(any(), any())).thenReturn(0L);
        // Mimics DB-generated ids: a mocked save() otherwise leaves ManagerAccess.getId() null, unlike production Hibernate.
        lenient().when(managerAccessRepository.save(any())).thenAnswer(invocation ->
                TestEntities.withId(invocation.getArgument(0), UUID.randomUUID()));
    }

    private InviteManagerRequest inviteRequest(String email) {
        return new InviteManagerRequest(email, Set.of(Permission.MANAGE_MENU));
    }

    @Test
    void invitingANewEmailCreatesAPendingInvitationEvenWithNoExistingAccount() {
        when(accountRepository.findByEmailIgnoreCase("new-manager@example.com")).thenReturn(Optional.empty());

        ManagerAccess access = managerService.invite(websiteId, owner, inviteRequest("new-manager@example.com"));

        assertThat(access.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(access.getManagerAccount()).isNull();
    }

    @Test
    void invitingAnEmailWithAnExistingAccountLinksItButStaysPendingUntilAccepted() {
        Account existing = TestEntities.withId(new Account(), UUID.randomUUID());
        existing.setEmail("manager@example.com");
        when(accountRepository.findByEmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(existing));

        ManagerAccess access = managerService.invite(websiteId, owner, inviteRequest("manager@example.com"));

        // This is the exact rule Phase 4 fixes: an existing account must NOT cause automatic acceptance.
        assertThat(access.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(access.getManagerAccount()).isEqualTo(existing);
    }

    @Test
    void cannotInviteTheWebsiteOwnerThemselves() {
        assertThatThrownBy(() -> managerService.invite(websiteId, owner, inviteRequest("owner@example.com")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void cannotInviteTheSameEmailTwiceWhileAnInvitationIsActive() {
        when(managerAccessRepository.existsByWebsiteIdAndInvitedEmailIgnoreCaseAndStatusIn(
                websiteId, "manager@example.com", EnumSet.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED)))
                .thenReturn(true);

        assertThatThrownBy(() -> managerService.invite(websiteId, owner, inviteRequest("manager@example.com")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already");
    }

    @Test
    void cannotInviteWithoutAnActiveSubscription() {
        when(subscriptionQueryService.getActivePlan(websiteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> managerService.invite(websiteId, owner, inviteRequest("manager@example.com")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("subscription");
    }

    @Test
    void cannotExceedThePlansManagerLimit() {
        when(managerAccessRepository.countByWebsiteIdAndStatusIn(eq(websiteId), any())).thenReturn(2L); // plan.maxManagersPerWebsite = 2

        assertThatThrownBy(() -> managerService.invite(websiteId, owner, inviteRequest("manager@example.com")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("plan allows up to");
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void linksPendingInvitationsButKeepsThemPendingUntilExplicitlyAccepted() {
        Account newAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        newAccount.setEmail("manager@example.com");
        newAccount.setRole(Role.MANAGER);

        ManagerAccess pendingInvite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        pendingInvite.setInvitedEmail("manager@example.com");
        pendingInvite.setStatus(InvitationStatus.PENDING);
        pendingInvite.setWebsite(website);

        when(managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus("manager@example.com", InvitationStatus.PENDING))
                .thenReturn(List.of(pendingInvite));

        managerService.linkPendingInvitationsForNewAccount(newAccount);

        // Linking happens immediately so the invite shows up for them - but it must stay PENDING (BR-MGR-008).
        assertThat(pendingInvite.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(pendingInvite.getManagerAccount()).isEqualTo(newAccount);
    }

    @Test
    void doesNothingWhenNoInvitationsAreWaitingForThisEmail() {
        Account newAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        newAccount.setEmail("nobody-invited-this-address@example.com");

        when(managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus(newAccount.getEmail(), InvitationStatus.PENDING))
                .thenReturn(List.of());

        managerService.linkPendingInvitationsForNewAccount(newAccount);
        // No exception, nothing to assert beyond "it didn't blow up" - the interaction above is the real check.
    }

    @Test
    void managerCanAcceptTheirOwnPendingInvitation() {
        Account managerAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        AuthenticatedAccount manager = new AuthenticatedAccount(managerAccount.getId(), "manager@example.com", Role.MANAGER);

        ManagerAccess invite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        invite.setManagerAccount(managerAccount);
        invite.setStatus(InvitationStatus.PENDING);
        when(managerAccessRepository.findById(invite.getId())).thenReturn(Optional.of(invite));

        ManagerAccess accepted = managerService.accept(invite.getId(), manager);

        assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void managerCanRejectTheirOwnPendingInvitation() {
        Account managerAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        AuthenticatedAccount manager = new AuthenticatedAccount(managerAccount.getId(), "manager@example.com", Role.MANAGER);

        ManagerAccess invite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        invite.setManagerAccount(managerAccount);
        invite.setStatus(InvitationStatus.PENDING);
        when(managerAccessRepository.findById(invite.getId())).thenReturn(Optional.of(invite));

        managerService.reject(invite.getId(), manager);

        assertThat(invite.getStatus()).isEqualTo(InvitationStatus.REJECTED);
    }

    @Test
    void cannotAcceptSomeoneElsesInvitation() {
        Account managerAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        AuthenticatedAccount imposter = new AuthenticatedAccount(UUID.randomUUID(), "imposter@example.com", Role.MANAGER);

        ManagerAccess invite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        invite.setManagerAccount(managerAccount);
        invite.setStatus(InvitationStatus.PENDING);
        when(managerAccessRepository.findById(invite.getId())).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> managerService.accept(invite.getId(), imposter))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void cannotAcceptAnInvitationThatIsNoLongerPending() {
        Account managerAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        AuthenticatedAccount manager = new AuthenticatedAccount(managerAccount.getId(), "manager@example.com", Role.MANAGER);

        ManagerAccess invite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        invite.setManagerAccount(managerAccount);
        invite.setStatus(InvitationStatus.EXPIRED);
        when(managerAccessRepository.findById(invite.getId())).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> managerService.accept(invite.getId(), manager))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void revokeSoftDeletesInsteadOfRemovingTheRow() {
        ManagerAccess access = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        access.setWebsite(website);
        access.setStatus(InvitationStatus.ACCEPTED);
        when(managerAccessRepository.findById(access.getId())).thenReturn(Optional.of(access));

        managerService.revoke(websiteId, access.getId(), owner);

        assertThat(access.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        org.mockito.Mockito.verify(managerAccessRepository, org.mockito.Mockito.never()).delete(any());
    }
}
