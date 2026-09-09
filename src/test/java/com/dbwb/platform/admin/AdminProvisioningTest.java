package com.dbwb.platform.admin;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.admin.dto.ProvisionWebsiteRequest;
import com.dbwb.platform.admin.dto.ProvisionedWebsiteResponse;
import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.audit.repository.AuditLogRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.config.FrontendProperties;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.repository.PlanRepository;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.subscription.repository.MockPaymentRepository;
import com.dbwb.platform.subscription.repository.SubscriptionRepository;
import com.dbwb.platform.support.SupportService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.theme.ThemeConfigValidator;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.website.SlugGenerator;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A Super Admin standing a website up for somebody else - including the case
 * where that somebody does not have an account yet, and the case where the
 * platform is not going to charge them.
 */
@ExtendWith(MockitoExtension.class)
class AdminProvisioningTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SlugGenerator slugGenerator;
    @Mock private BusinessProfileRepository profileRepository;
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

    private final AuthenticatedAccount superAdmin =
            new AuthenticatedAccount(UUID.randomUUID(), "admin@example.com", Role.SUPER_ADMIN);
    private final AuthenticatedAccount owner =
            new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                accountRepository, tokenRepository, passwordEncoder, slugGenerator,
                new BusinessRuleProperties(), new FrontendProperties(), profileRepository,
                websiteRepository, themeRepository, themeConfigValidator, planRepository, templatePriceRepository,
                subscriptionRepository, mockPaymentRepository, supportService, emailService,
                auditService, auditLogRepository);

        lenient().when(slugGenerator.generateUniqueSlug(anyString())).thenReturn("the-corner-shop");
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        lenient().when(websiteRepository.save(any(BusinessWebsite.class)))
                .thenAnswer(i -> TestEntities.withId(i.getArgument(0), UUID.randomUUID()));
        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(i -> TestEntities.withId(i.getArgument(0), UUID.randomUUID()));

        Plan basicMonthly = new Plan();
        basicMonthly.setCode(PlanCode.BASIC);
        basicMonthly.setBillingPeriod(BillingPeriod.MONTHLY);
        basicMonthly.setPrice(new BigDecimal("9.99"));
        lenient().when(planRepository.findByCodeAndBillingPeriod(PlanCode.BASIC, BillingPeriod.MONTHLY))
                .thenReturn(Optional.of(basicMonthly));
    }

    private ProvisionWebsiteRequest request(boolean complimentary) {
        return new ProvisionWebsiteRequest("client@example.com", "Client Name", "The Corner Shop",
                TemplateType.MENU_ORDERING, null, null, complimentary);
    }

    @Test
    void onlyASuperAdminMayProvision() {
        assertThatThrownBy(() -> adminService.provisionWebsiteForOwner(owner, request(false)))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void anUnknownEmailGetsAnAccountAndAnInvitation() {
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());

        ProvisionedWebsiteResponse result = adminService.provisionWebsiteForOwner(superAdmin, request(false));

        assertThat(result.ownerAccountCreated()).isTrue();
        ArgumentCaptor<Account> created = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(created.capture());
        assertThat(created.getValue().getRole()).isEqualTo(Role.BUSINESS_OWNER);
        assertThat(created.getValue().getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        // The invitation is what lets them choose a password; the admin never picks one.
        verify(tokenRepository).save(any());
        verify(emailService).send(eq("client@example.com"), anyString(), anyString());
    }

    @Test
    void aKnownEmailReusesTheAccountRatherThanMakingASecond() {
        Account existing = TestEntities.withId(new Account(), UUID.randomUUID());
        existing.setEmail("client@example.com");
        existing.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.of(existing));

        ProvisionedWebsiteResponse result = adminService.provisionWebsiteForOwner(superAdmin, request(false));

        assertThat(result.ownerAccountCreated()).isFalse();
        assertThat(result.ownerId()).isEqualTo(existing.getId());
        verify(accountRepository, never()).save(any(Account.class));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void aDeletedAccountIsRefusedRatherThanQuietlyRevived() {
        Account deleted = TestEntities.withId(new Account(), UUID.randomUUID());
        deleted.setEmail("client@example.com");
        deleted.setStatus(AccountStatus.DELETED);
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> adminService.provisionWebsiteForOwner(superAdmin, request(false)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("deleted");
    }

    @Test
    void theWebsiteStartsAsADraftOwnedByThatPerson() {
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());

        adminService.provisionWebsiteForOwner(superAdmin, request(false));

        ArgumentCaptor<BusinessWebsite> saved = ArgumentCaptor.forClass(BusinessWebsite.class);
        verify(websiteRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(WebsiteStatus.DRAFT);
        assertThat(saved.getValue().getBusinessName()).isEqualTo("The Corner Shop");
        assertThat(saved.getValue().getOwner().getEmail()).isEqualTo("client@example.com");
    }

    @Test
    void withoutComplimentaryNoSubscriptionIsCreated() {
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());

        adminService.provisionWebsiteForOwner(superAdmin, request(false));

        // It gets the same free trial as anybody else, at its first publish.
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void complimentaryGrantsFreeAccessThatNeverExpires() {
        when(accountRepository.findByEmailIgnoreCase("client@example.com")).thenReturn(Optional.empty());

        adminService.provisionWebsiteForOwner(superAdmin, request(true));

        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(saved.capture());
        Subscription free = saved.getValue();
        assertThat(free.isComplimentary()).isTrue();
        assertThat(free.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        // Nothing to expire and nothing to chase: the lifecycle job looks for an
        // end date, and there is none.
        assertThat(free.getEndDate()).isNull();
        assertThat(free.getGraceEndsAt()).isNull();
    }

    @Test
    void aTemplateFromTheOtherFamilyIsRefused() {
        ProvisionWebsiteRequest mismatched = new ProvisionWebsiteRequest(
                "client@example.com", null, "The Corner Shop",
                TemplateType.MENU_ORDERING, LayoutVariant.PORTFOLIO_PROFESSIONAL, null, false);

        assertThatThrownBy(() -> adminService.provisionWebsiteForOwner(superAdmin, mismatched))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not belong");
    }
}
