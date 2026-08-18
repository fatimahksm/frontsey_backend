package com.dbwb.platform.admin;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.config.FrontendProperties;
import com.dbwb.platform.website.SlugGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.dbwb.platform.admin.dto.AdminWebsiteUpdateRequest;
import com.dbwb.platform.admin.dto.UpdateUserRoleRequest;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.audit.entity.AuditLog;
import com.dbwb.platform.audit.repository.AuditLogRepository;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.support.SupportService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private BusinessProfileRepository profileRepository;
    @Mock private AccountTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SlugGenerator slugGenerator;
    @Mock private BusinessWebsiteRepository websiteRepository;
    @Mock private ThemeRepository themeRepository;
    @Mock private ThemeConfigValidator themeConfigValidator;
    @Mock private PlanRepository planRepository;
    @Mock private TemplatePriceRepository templatePriceRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MockPaymentRepository mockPaymentRepository;
    @Mock private SupportService supportService;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private AuditLogRepository auditLogRepository;

    private AdminService adminService;

    private final UUID accountId = UUID.randomUUID();
    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount superAdmin = new AuthenticatedAccount(UUID.randomUUID(), "admin@example.com", Role.SUPER_ADMIN);
    private final AuthenticatedAccount businessOwner = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                accountRepository,
                tokenRepository, passwordEncoder, slugGenerator,
                new BusinessRuleProperties(), new FrontendProperties(),
                profileRepository, websiteRepository, themeRepository, themeConfigValidator, planRepository, templatePriceRepository, subscriptionRepository,
                mockPaymentRepository, supportService, emailService, auditService, auditLogRepository);
    }

    private Account existingAccount() {
        Account account = TestEntities.withId(new Account(), accountId);
        account.setEmail("user@example.com");
        account.setRole(Role.BUSINESS_OWNER);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private BusinessWebsite existingWebsite() {
        BusinessWebsite website = TestEntities.withId(new BusinessWebsite(), websiteId);
        website.setBusinessName("Old Name");
        website.setStatus(WebsiteStatus.PUBLISHED);
        Account owner = new Account();
        owner.setEmail("owner@example.com");
        website.setOwner(owner);
        return website;
    }

    @Test
    void nonSuperAdminCannotUpdateUserRole() {
        assertThatThrownBy(() -> adminService.updateUserRole(accountId, businessOwner, new UpdateUserRoleRequest(Role.MANAGER)))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void superAdminCanPromoteAUserToManager() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount()));

        Account updated = adminService.updateUserRole(accountId, superAdmin, new UpdateUserRoleRequest(Role.MANAGER));

        assertThat(updated.getRole()).isEqualTo(Role.MANAGER);
        verify(auditService).record(superAdmin.accountId(), "USER_ROLE_UPDATED", accountId + " -> MANAGER");
    }

    @Test
    void disablingAUserSetsTheSameLifecycleStateAsSelfServiceDeletion() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount()));

        Account disabled = adminService.disableUser(accountId, superAdmin);

        assertThat(disabled.getStatus()).isEqualTo(AccountStatus.DISABLED_PENDING_DELETION);
        assertThat(disabled.getDisabledAt()).isNotNull();
    }

    @Test
    void reactivatingAnAccountThatIsNotDisabledFails() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount()));

        assertThatThrownBy(() -> adminService.reactivateUser(accountId, superAdmin))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void superAdminCanEditAWebsitesBusinessNameDirectly() {
        when(websiteRepository.findById(websiteId)).thenReturn(Optional.of(existingWebsite()));

        BusinessWebsite updated = adminService.updateWebsiteDetails(websiteId, superAdmin, new AdminWebsiteUpdateRequest("New Name"));

        assertThat(updated.getBusinessName()).isEqualTo("New Name");
    }

    @Test
    void superAdminCanDeleteAWebsiteDirectly() {
        when(websiteRepository.findById(websiteId)).thenReturn(Optional.of(existingWebsite()));

        adminService.deleteWebsite(websiteId, superAdmin);

        verify(auditService).record(superAdmin.accountId(), "WEBSITE_DELETED_BY_ADMIN", websiteId.toString());
    }

    @Test
    void listAuditLogsAttachesTheActorsEmail() {
        AuditLog log = TestEntities.withId(new AuditLog(), UUID.randomUUID());
        log.setActorAccountId(accountId);
        log.setAction("WEBSITE_CREATED");
        log.setTargetId(websiteId.toString());
        lenient().when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log));
        lenient().when(accountRepository.findAllById(List.of(accountId))).thenReturn(List.of(existingAccount()));

        var results = adminService.listAuditLogs(superAdmin);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).actorEmail()).isEqualTo("user@example.com");
    }
}
