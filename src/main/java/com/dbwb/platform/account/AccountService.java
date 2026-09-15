package com.dbwb.platform.account;

import com.dbwb.platform.account.dto.AccountDataExportResponse;
import com.dbwb.platform.account.dto.AccountProfileResponse;
import com.dbwb.platform.account.dto.ChangePasswordRequest;
import com.dbwb.platform.account.dto.UpdateAccountProfileRequest;
import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.delivery.dto.DeliveryAreaResponse;
import com.dbwb.platform.delivery.repository.DeliveryAreaRepository;
import com.dbwb.platform.menu.dto.CategoryDto;
import com.dbwb.platform.menu.dto.MenuItemResponse;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.profile.dto.BusinessProfileResponse;
import com.dbwb.platform.profile.dto.OpeningHoursEntry;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.profile.repository.OpeningHoursRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.dto.WebsiteResponse;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * BR-AUTH-006: two-step account deletion (disable, then permanently delete
 * after a configured window - recoverable via cancelDeletion until then) plus
 * the data export the Owner must be offered first (BR-DATA-005).
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BusinessWebsiteRepository websiteRepository;
    private final BusinessProfileRepository profileRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;
    private final PasswordEncoder passwordEncoder;
    private final BusinessRuleProperties businessRules;
    private final EmailService emailService;

    public AccountService(
            AccountRepository accountRepository,
            BusinessWebsiteRepository websiteRepository,
            BusinessProfileRepository profileRepository,
            OpeningHoursRepository openingHoursRepository,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            DeliveryAreaRepository deliveryAreaRepository,
            PasswordEncoder passwordEncoder,
            BusinessRuleProperties businessRules,
            EmailService emailService) {
        this.accountRepository = accountRepository;
        this.websiteRepository = websiteRepository;
        this.profileRepository = profileRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.deliveryAreaRepository = deliveryAreaRepository;
        this.passwordEncoder = passwordEncoder;
        this.businessRules = businessRules;
        this.emailService = emailService;
    }

    /**
     * Who the caller is signed in as.
     *
     * Read from the database rather than assembled from the token: a token is
     * minted at sign-in and outlives changes made since, so a name changed ten
     * seconds ago would still read as the old one on the screen that changed
     * it.
     */
    @Transactional(readOnly = true)
    public AccountProfileResponse profile(AuthenticatedAccount caller) {
        return AccountProfileResponse.from(load(caller.accountId()));
    }

    @Transactional
    public AccountProfileResponse updateProfile(AuthenticatedAccount caller, UpdateAccountProfileRequest request) {
        Account account = load(caller.accountId());
        account.setFullName(request.fullName().trim());
        return AccountProfileResponse.from(account);
    }

    /**
     * Changes the password of an already-authenticated account.
     *
     * The current password is checked even though the caller holds a valid
     * session. A session on a machine somebody walked away from is precisely
     * the case this stops; without the check, whoever found that browser could
     * take the account away from the person who owns the business.
     *
     * The failure message does not distinguish a wrong current password from
     * anything else about the account, and the new password is rejected when
     * it matches the old one - a "change" that changes nothing is a false
     * sense of having reacted to something.
     */
    @Transactional
    public void changePassword(AuthenticatedAccount caller, ChangePasswordRequest request) {
        Account account = load(caller.accountId());
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("That is not your current password.");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("Your new password must be different from your current one.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));

        // Told, not asked: a password changing is the one account event whose
        // owner must hear about it even when they are the one who did it,
        // because the time they did not is the time it matters.
        emailService.send(account.getEmail(), "Your password was changed",
                "The password for your Frontsey account was just changed. "
                        + "If that was not you, reset your password immediately and contact support.");
    }

    @Transactional
    public void requestDeletion(AuthenticatedAccount caller) {
        Account account = load(caller.accountId());
        account.setStatus(AccountStatus.DISABLED_PENDING_DELETION);
        account.setDisabledAt(Instant.now());

        emailService.send(account.getEmail(), "Your account is scheduled for deletion",
                "Your account has been disabled and will be permanently deleted in "
                        + businessRules.getAccountDeletionDisableWindowDays()
                        + " days. Export your data before then if you haven't already. "
                        + "Log back in before then to cancel.");
    }

    /** BR-AUTH-006 implies recoverability within the disable window (see BusinessRuleProperties javadoc). */
    @Transactional
    public void cancelDeletion(AuthenticatedAccount caller) {
        Account account = load(caller.accountId());
        if (account.getStatus() != AccountStatus.DISABLED_PENDING_DELETION) {
            throw new BusinessRuleViolationException("This account is not scheduled for deletion.");
        }
        account.setStatus(AccountStatus.ACTIVE);
        account.setDisabledAt(null);
    }

    @Transactional(readOnly = true)
    public AccountDataExportResponse exportData(AuthenticatedAccount caller) {
        Account account = load(caller.accountId());

        List<AccountDataExportResponse.WebsiteExport> websites = websiteRepository.findByOwnerId(account.getId()).stream()
                .map(website -> new AccountDataExportResponse.WebsiteExport(
                        WebsiteResponse.from(website),
                        profileRepository.findByWebsiteId(website.getId()).map(BusinessProfileResponse::from)
                                .orElseGet(BusinessProfileResponse::empty),
                        openingHoursRepository.findByWebsiteIdOrderByDayOfWeek(website.getId()).stream()
                                .map(OpeningHoursEntry::from).toList(),
                        categoryRepository.findByWebsiteId(website.getId()).stream().map(CategoryDto::from).toList(),
                        menuItemRepository.findByWebsiteIdAndTrashedAtIsNull(website.getId()).stream()
                                .map(MenuItemResponse::from).toList(),
                        deliveryAreaRepository.findByWebsiteId(website.getId()).stream()
                                .map(DeliveryAreaResponse::from).toList()))
                .toList();

        return new AccountDataExportResponse(account.getEmail(), Instant.now(), websites);
    }

    /**
     * Scheduled: anonymizes accounts past their disable window. Business data
     * (websites/menu/etc.) is intentionally left intact rather than
     * cascade-deleted here - it becomes inaccessible once the owning account's
     * credentials are gone, and website-level deletion already has its own
     * lifecycle (BR-DATA-004, website trash).
     */
    @Transactional
    public void permanentlyDeleteOverdueAccounts() {
        Instant cutoff = Instant.now().minus(businessRules.getAccountDeletionDisableWindowDays(), ChronoUnit.DAYS);
        accountRepository.findByStatusAndDisabledAtBefore(AccountStatus.DISABLED_PENDING_DELETION, cutoff)
                .forEach(account -> {
                    account.setEmail("deleted-" + account.getId() + "@deleted.invalid");
                    account.setFullName(null);
                    account.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    account.setStatus(AccountStatus.DELETED);
                });
    }

    private Account load(UUID accountId) {
        return accountRepository.findById(accountId).orElseThrow();
    }
}
