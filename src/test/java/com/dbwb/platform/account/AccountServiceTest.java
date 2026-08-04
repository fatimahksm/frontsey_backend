package com.dbwb.platform.account;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.profile.repository.OpeningHoursRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BusinessWebsiteRepository websiteRepository;
    @Mock
    private BusinessProfileRepository profileRepository;
    @Mock
    private OpeningHoursRepository openingHoursRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private DeliveryAreaRepository deliveryAreaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BusinessRuleProperties businessRules;
    @Mock
    private EmailService emailService;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository, websiteRepository, profileRepository, openingHoursRepository,
                categoryRepository, menuItemRepository, deliveryAreaRepository, passwordEncoder, businessRules, emailService);
    }

    @Test
    void requestDeletionDisablesTheAccountAndRecordsWhen() {
        Account account = activeAccount();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        accountService.requestDeletion(caller(account));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.DISABLED_PENDING_DELETION);
        assertThat(account.getDisabledAt()).isNotNull();
    }

    @Test
    void cancelDeletionRestoresAnActiveDisabledPendingDeletionAccount() {
        Account account = activeAccount();
        account.setStatus(AccountStatus.DISABLED_PENDING_DELETION);
        account.setDisabledAt(Instant.now());
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        accountService.cancelDeletion(caller(account));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getDisabledAt()).isNull();
    }

    @Test
    void cancelDeletionRejectsAnAccountThatIsNotScheduledForDeletion() {
        Account account = activeAccount();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.cancelDeletion(caller(account)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not scheduled for deletion");
    }

    @Test
    void permanentlyDeletingAnOverdueAccountAnonymizesRatherThanHardDeletes() {
        // Business data (websites/menu/etc.) is intentionally left intact - see
        // AccountService javadoc. Anonymizing keeps the FK-referenced row valid
        // without a risky cascade delete across a dozen tables.
        Account overdue = activeAccount();
        overdue.setStatus(AccountStatus.DISABLED_PENDING_DELETION);
        overdue.setDisabledAt(Instant.now().minus(31, ChronoUnit.DAYS));
        String originalEmail = overdue.getEmail();

        lenient().when(businessRules.getAccountDeletionDisableWindowDays()).thenReturn(30);
        when(accountRepository.findByStatusAndDisabledAtBefore(any(), any())).thenReturn(List.of(overdue));
        when(passwordEncoder.encode(any())).thenReturn("unusable-hash");

        accountService.permanentlyDeleteOverdueAccounts();

        assertThat(overdue.getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(overdue.getEmail()).isNotEqualTo(originalEmail);
        assertThat(overdue.getFullName()).isNull();
        assertThat(overdue.getPasswordHash()).isEqualTo("unusable-hash");
    }

    private Account activeAccount() {
        Account account = new Account();
        account.setEmail("owner@example.com");
        account.setFullName("Owner");
        account.setRole(Role.BUSINESS_OWNER);
        account.setStatus(AccountStatus.ACTIVE);
        account.setEmailVerified(true);
        return TestEntities.withId(account, UUID.randomUUID());
    }

    private AuthenticatedAccount caller(Account account) {
        return new AuthenticatedAccount(account.getId(), account.getEmail(), account.getRole());
    }
}
