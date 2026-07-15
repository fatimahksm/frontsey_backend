package com.dbwb.platform.manager;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * This is the exact bug this session found: linkPendingInvitationsForNewAccount
 * queried by managerAccountId (always null for a PENDING invite - that field
 * only gets set once a manager is linked) so it could never match anything.
 * These tests pin down the fixed behavior: lookup by invitedEmail instead.
 */
@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerAccessRepository managerAccessRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private EmailService emailService;
    @Mock
    private AuditService auditService;
    @Mock
    private BusinessRuleProperties businessRules;

    private ManagerService managerService;

    @Test
    void linksAllPendingInvitationsMatchingTheNewAccountsEmail() {
        managerService = new ManagerService(managerAccessRepository, accountRepository, accessGuard, emailService, auditService, businessRules);

        Account newAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        newAccount.setEmail("manager@example.com");
        newAccount.setRole(Role.MANAGER);

        ManagerAccess pendingInvite = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        pendingInvite.setInvitedEmail("manager@example.com");
        pendingInvite.setStatus(InvitationStatus.PENDING);

        when(managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus("manager@example.com", InvitationStatus.PENDING))
                .thenReturn(List.of(pendingInvite));

        managerService.linkPendingInvitationsForNewAccount(newAccount);

        assertThat(pendingInvite.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(pendingInvite.getManagerAccount()).isEqualTo(newAccount);
    }

    @Test
    void doesNothingWhenNoInvitationsAreWaitingForThisEmail() {
        managerService = new ManagerService(managerAccessRepository, accountRepository, accessGuard, emailService, auditService, businessRules);

        Account newAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        newAccount.setEmail("nobody-invited-this-address@example.com");

        when(managerAccessRepository.findByInvitedEmailIgnoreCaseAndStatus(newAccount.getEmail(), InvitationStatus.PENDING))
                .thenReturn(List.of());

        managerService.linkPendingInvitationsForNewAccount(newAccount);
        // No exception, nothing to assert beyond "it didn't blow up" - the interaction above is the real check.
    }
}
