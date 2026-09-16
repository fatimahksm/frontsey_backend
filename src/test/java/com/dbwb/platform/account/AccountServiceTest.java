package com.dbwb.platform.account;

import com.dbwb.platform.account.dto.ChangePasswordRequest;
import com.dbwb.platform.account.dto.UpdateAccountProfileRequest;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // --- who you are signed in as, and changing your own password ---

    /**
     * The Account screen offered to export and permanently delete an account
     * it could not name, and a Super Admin had no route to it at all. This is
     * the read that lets a screen say whose account it is.
     */
    @Test
    void profileReportsTheSignedInAccount() {
        Account account = activeAccount();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        var profile = accountService.profile(caller(account));

        assertThat(profile.email()).isEqualTo("owner@example.com");
        assertThat(profile.fullName()).isEqualTo("Owner");
        assertThat(profile.role()).isEqualTo(Role.BUSINESS_OWNER);
        assertThat(profile.emailVerified()).isTrue();
    }

    @Test
    void updatingTheProfileChangesTheNameAndTrimsIt() {
        Account account = activeAccount();
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        var updated = accountService.updateProfile(caller(account), new UpdateAccountProfileRequest("  Rania Khoury  "));

        assertThat(updated.fullName()).isEqualTo("Rania Khoury");
        assertThat(account.getFullName()).isEqualTo("Rania Khoury");
    }

    @Test
    void changingThePasswordStoresANewHash() {
        Account account = activeAccount();
        account.setPasswordHash("old-hash");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("brand-new-one", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("brand-new-one")).thenReturn("new-hash");

        accountService.changePassword(caller(account), new ChangePasswordRequest("current", "brand-new-one"));

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
    }

    /**
     * The check that matters. The caller already holds a valid session, so
     * without this anyone who found a signed-in browser on an unattended
     * machine could take the account away from the business that owns it.
     */
    @Test
    void refusesToChangeThePasswordWithoutTheCurrentOne() {
        Account account = activeAccount();
        account.setPasswordHash("old-hash");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> accountService.changePassword(
                caller(account), new ChangePasswordRequest("wrong", "brand-new-one")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not your current password");
        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void refusesANewPasswordThatIsTheSameAsTheOldOne() {
        Account account = activeAccount();
        account.setPasswordHash("old-hash");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> accountService.changePassword(
                caller(account), new ChangePasswordRequest("current", "current")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("different");
    }

    /**
     * A password changing is the one account event its owner has to hear about
     * even when they did it themselves - because the time they did not is the
     * time it matters.
     */
    @Test
    void tellsTheOwnerTheirPasswordChanged() {
        Account account = activeAccount();
        account.setPasswordHash("old-hash");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("brand-new-one", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("brand-new-one")).thenReturn("new-hash");

        accountService.changePassword(caller(account), new ChangePasswordRequest("current", "brand-new-one"));

        verify(emailService).send(eq("owner@example.com"), contains("password"), any());
    }

    @Test
    void sendsNoEmailWhenTheChangeIsRefused() {
        Account account = activeAccount();
        account.setPasswordHash("old-hash");
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> accountService.changePassword(
                caller(account), new ChangePasswordRequest("wrong", "brand-new-one")))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(emailService, never()).send(any(), any(), any());
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
