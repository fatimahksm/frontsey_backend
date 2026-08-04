package com.dbwb.platform.account;

import com.dbwb.platform.account.dto.AccountDataExportResponse;
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
