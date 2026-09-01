package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.profile.entity.BusinessProfile;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.publicapi.PublicWebsiteService;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.subscription.SubscriptionService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.OrderingMode;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.dto.CreateWebsiteRequest;
import com.dbwb.platform.website.dto.UpdateDraftContentRequest;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * BR-THEME-006, now branched by TemplateType: a MENU_ORDERING website needs
 * at least one menu category, a PORTFOLIO website needs at least one
 * service instead - the two content models are mutually exclusive.
 */
@ExtendWith(MockitoExtension.class)
class WebsiteServiceTest {

    @Mock private BusinessWebsiteRepository websiteRepository;
    @Mock private ThemeRepository themeRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private BusinessProfileRepository profileRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ServiceItemRepository serviceItemRepository;
    @Mock private SlugGenerator slugGenerator;
    @Mock private WebsiteAccessGuard accessGuard;
    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private com.dbwb.platform.audit.AuditService auditService;
    @Mock private PublicWebsiteService publicWebsiteService;
    @Mock private ManagerAccessRepository managerAccessRepository;
    @Mock private com.dbwb.platform.theme.ThemeConfigValidator themeConfigValidator;
    @Mock private com.dbwb.platform.plan.TemplateAvailability templateAvailability;
    private final com.dbwb.platform.common.config.BusinessRuleProperties businessRules =
            new com.dbwb.platform.common.config.BusinessRuleProperties();

    private WebsiteService websiteService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount owner = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        websiteService = new WebsiteService(
                websiteRepository, themeRepository, accountRepository, profileRepository, categoryRepository,
                serviceItemRepository, slugGenerator, accessGuard, subscriptionQueryService, subscriptionService,
                auditService, publicWebsiteService, managerAccessRepository, themeConfigValidator,
                templateAvailability, businessRules);

        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        website.setBusinessName("Test Business");
        website.setPageMode(PageMode.ONE_PAGE);
        website.setStatus(WebsiteStatus.DRAFT);

        lenient().when(accessGuard.requirePermission(eq(websiteId), eq(owner), any())).thenReturn(website);
        lenient().when(subscriptionQueryService.hasPublishableSubscription(websiteId)).thenReturn(true);
        lenient().when(profileRepository.findByWebsiteId(websiteId))
                .thenReturn(Optional.of(TestEntities.withId(new BusinessProfile(), UUID.randomUUID())));
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    @Test
    void updateThemeConfigStoresTheOverrideAfterValidatingIt() {
        when(accessGuard.requirePermission(websiteId, owner, Permission.MANAGE_THEME_AND_CONTENT)).thenReturn(website);

        websiteService.updateThemeConfig(websiteId, owner, "{\"primaryColor\":\"#123456\"}");

        assertThat(website.getThemeConfig()).isEqualTo("{\"primaryColor\":\"#123456\"}");
        verify(themeConfigValidator).parseAndValidate("{\"primaryColor\":\"#123456\"}");
    }

    @Test
    void updateThemeConfigWithNullClearsTheOverrideSoThePresetAppliesAgain() {
        website.setThemeConfig("{\"primaryColor\":\"#123456\"}");
        when(accessGuard.requirePermission(websiteId, owner, Permission.MANAGE_THEME_AND_CONTENT)).thenReturn(website);

        websiteService.updateThemeConfig(websiteId, owner, null);

        assertThat(website.getThemeConfig()).isNull();
        // Nothing to validate when the override is being removed.
        verifyNoInteractions(themeConfigValidator);
    }

    @Test
    void menuOrderingWebsiteCannotPublishWithoutAtLeastOneCategory() {
        website.setTemplateType(TemplateType.MENU_ORDERING);
        when(categoryRepository.countByWebsiteId(websiteId)).thenReturn(0L);

        assertThatThrownBy(() -> websiteService.publish(websiteId, owner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("menu category");
    }

    @Test
    void menuOrderingWebsitePublishesOnceItHasACategory() {
        website.setTemplateType(TemplateType.MENU_ORDERING);
        when(categoryRepository.countByWebsiteId(websiteId)).thenReturn(1L);

        BusinessWebsite published = websiteService.publish(websiteId, owner);

        assertThat(published.getStatus()).isEqualTo(WebsiteStatus.PUBLISHED);
    }

    @Test
    void portfolioWebsiteCannotPublishWithoutAtLeastOneService() {
        website.setTemplateType(TemplateType.PORTFOLIO);
        when(serviceItemRepository.countByWebsiteId(websiteId)).thenReturn(0L);

        assertThatThrownBy(() -> websiteService.publish(websiteId, owner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("service");
    }

    @Test
    void portfolioWebsitePublishesOnceItHasAService() {
        website.setTemplateType(TemplateType.PORTFOLIO);
        when(serviceItemRepository.countByWebsiteId(websiteId)).thenReturn(1L);

        BusinessWebsite published = websiteService.publish(websiteId, owner);

        assertThat(published.getStatus()).isEqualTo(WebsiteStatus.PUBLISHED);
    }

    @Test
    void portfolioWebsitePublishDoesNotConsultCategoryCountAtAll() {
        website.setTemplateType(TemplateType.PORTFOLIO);
        when(serviceItemRepository.countByWebsiteId(websiteId)).thenReturn(1L);

        websiteService.publish(websiteId, owner);

        org.mockito.Mockito.verifyNoInteractions(categoryRepository);
    }

    @Test
    void unsetLayoutVariantDefaultsToClassicForMenuOrdering() {
        website.setTemplateType(TemplateType.MENU_ORDERING);

        assertThat(website.getEffectiveLayoutVariant()).isEqualTo(LayoutVariant.MENU_CLASSIC);
    }

    @Test
    void unsetLayoutVariantDefaultsToHeroForPortfolio() {
        website.setTemplateType(TemplateType.PORTFOLIO);

        assertThat(website.getEffectiveLayoutVariant()).isEqualTo(LayoutVariant.PORTFOLIO_PROFESSIONAL);
    }

    @Test
    void ownerCanSwitchToTheOtherValidLayoutForTheirTemplateType() {
        website.setTemplateType(TemplateType.MENU_ORDERING);

        BusinessWebsite updated = websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.MENU_GRID);

        assertThat(updated.getEffectiveLayoutVariant()).isEqualTo(LayoutVariant.MENU_GRID);
    }

    @Test
    void switchingToTheCartLessClassicLayoutPinsTheWebsiteToDisplayOnly() {
        website.setTemplateType(TemplateType.MENU_ORDERING);
        website.setOrderingMode(OrderingMode.WHATSAPP_ORDERING);

        BusinessWebsite updated = websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.MENU_CLASSIC);

        assertThat(updated.getOrderingMode()).isEqualTo(OrderingMode.DISPLAY_ONLY);
    }

    @Test
    void savingADraftCannotReEnableOrderingOnACartLessLayout() {
        website.setTemplateType(TemplateType.MENU_ORDERING);
        website.setLayoutVariant(LayoutVariant.MENU_CLASSIC);

        BusinessWebsite updated = websiteService.saveDraft(
                websiteId, owner, new UpdateDraftContentRequest("{}", OrderingMode.WHATSAPP_ORDERING));

        assertThat(updated.getOrderingMode()).isEqualTo(OrderingMode.DISPLAY_ONLY);
    }

    @Test
    void savingADraftKeepsTheRequestedOrderingModeOnACartLayout() {
        website.setTemplateType(TemplateType.MENU_ORDERING);
        website.setLayoutVariant(LayoutVariant.MENU_GRID);

        BusinessWebsite updated = websiteService.saveDraft(
                websiteId, owner, new UpdateDraftContentRequest("{}", OrderingMode.WHATSAPP_ORDERING));

        assertThat(updated.getOrderingMode()).isEqualTo(OrderingMode.WHATSAPP_ORDERING);
    }

    @Test
    void cannotSwitchToALayoutThatBelongsToTheOtherTemplateType() {
        website.setTemplateType(TemplateType.MENU_ORDERING);

        assertThatThrownBy(() -> websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.PORTFOLIO_PROFESSIONAL))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not a valid layout");
    }

    // --- Phase 4: accessible-websites / caller role+permissions ---

    @Test
    void getWithAccessReturnsOwnerRoleForTheWebsiteOwner() {
        Account ownerAccount = TestEntities.withId(new Account(), owner.accountId());
        website.setOwner(ownerAccount);
        when(accessGuard.requireReadAccess(websiteId, owner)).thenReturn(website);

        var access = websiteService.getWithAccess(websiteId, owner);

        assertThat(access.role()).isEqualTo(com.dbwb.platform.website.dto.AccessRole.OWNER);
        assertThat(access.permissions()).isEmpty();
    }

    @Test
    void getWithAccessReturnsManagerRoleAndGrantedPermissions() {
        Account ownerAccount = TestEntities.withId(new Account(), UUID.randomUUID());
        website.setOwner(ownerAccount);
        AuthenticatedAccount manager = new AuthenticatedAccount(UUID.randomUUID(), "manager@example.com", Role.MANAGER);
        when(accessGuard.requireReadAccess(websiteId, manager)).thenReturn(website);

        ManagerAccess managerAccess = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        managerAccess.setPermissions(Set.of(Permission.MANAGE_MENU));
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, manager.accountId()))
                .thenReturn(Optional.of(managerAccess));

        var access = websiteService.getWithAccess(websiteId, manager);

        assertThat(access.role()).isEqualTo(com.dbwb.platform.website.dto.AccessRole.MANAGER);
        assertThat(access.permissions()).containsExactly(Permission.MANAGE_MENU);
    }

    @Test
    void listAccessibleCombinesOwnedAndAcceptedManagedWebsites() {
        BusinessWebsite owned = TestEntities.withId(new BusinessWebsite(), UUID.randomUUID());
        BusinessWebsite managed = TestEntities.withId(new BusinessWebsite(), UUID.randomUUID());
        when(websiteRepository.findByOwnerId(owner.accountId())).thenReturn(List.of(owned));

        ManagerAccess managerAccess = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        managerAccess.setWebsite(managed);
        managerAccess.setStatus(InvitationStatus.ACCEPTED);
        managerAccess.setPermissions(Set.of(Permission.VIEW_ANALYTICS));
        when(managerAccessRepository.findByManagerAccountIdAndStatus(owner.accountId(), InvitationStatus.ACCEPTED))
                .thenReturn(List.of(managerAccess));

        var result = websiteService.listAccessible(owner);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(a -> {
            assertThat(a.website()).isEqualTo(owned);
            assertThat(a.role()).isEqualTo(com.dbwb.platform.website.dto.AccessRole.OWNER);
        });
        assertThat(result).anySatisfy(a -> {
            assertThat(a.website()).isEqualTo(managed);
            assertThat(a.role()).isEqualTo(com.dbwb.platform.website.dto.AccessRole.MANAGER);
            assertThat(a.permissions()).containsExactly(Permission.VIEW_ANALYTICS);
        });
    }

    @Test
    void refusesALayoutTheAdminHasWithdrawn() {
        website.setTemplateType(TemplateType.PORTFOLIO);
        website.setLayoutVariant(LayoutVariant.PORTFOLIO_PROFESSIONAL);
        when(accessGuard.requirePermission(eq(websiteId), eq(owner), any())).thenReturn(website);
        doThrow(new BusinessRuleViolationException("That template is not available to choose right now. Pick another one."))
                .when(templateAvailability).requireOffered(LayoutVariant.PORTFOLIO_VISUAL);

        assertThatThrownBy(() -> websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.PORTFOLIO_VISUAL))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not available to choose");

        assertThat(website.getLayoutVariant()).isEqualTo(LayoutVariant.PORTFOLIO_PROFESSIONAL);
    }

    @Test
    void doesNotTrapAWebsiteAlreadyOnAWithdrawnLayout() {
        // Withdrawing a template must not mean the sites already on it can never
        // save this screen again. Re-selecting what they are already on is
        // allowed, and never consults availability at all.
        website.setTemplateType(TemplateType.PORTFOLIO);
        website.setLayoutVariant(LayoutVariant.PORTFOLIO_VISUAL);
        when(accessGuard.requirePermission(eq(websiteId), eq(owner), any())).thenReturn(website);

        websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.PORTFOLIO_VISUAL);

        assertThat(website.getLayoutVariant()).isEqualTo(LayoutVariant.PORTFOLIO_VISUAL);
        verify(templateAvailability, never()).requireOffered(any());
    }

    @Test
    void refusesToCreateAKindOfWebsiteWithNothingOnOffer() {
        // Otherwise the new website falls back to LayoutVariant.defaultFor(),
        // which may itself be withdrawn - creating it straight onto something
        // the owner was never allowed to pick.
        doThrow(new BusinessRuleViolationException("No templates of that kind are available right now."))
                .when(templateAvailability).requireAnyOffered(TemplateType.PORTFOLIO);

        assertThatThrownBy(() -> websiteService.create(owner,
                new CreateWebsiteRequest("A Studio", PageMode.MULTI_PAGE, TemplateType.PORTFOLIO, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("No templates of that kind");

        verify(websiteRepository, never()).save(any());
    }

    @Test
    void refusesAnotherWebsiteOnceTheOwnersAllowanceIsSpent() {
        // BRD 7.2 / TBD-003. Unenforced until now, so anyone with an account
        // could create websites without limit.
        businessRules.setDefaultWebsitesPerOwner(2);
        when(websiteRepository.findByOwnerId(owner.accountId()))
                .thenReturn(List.of(liveWebsite(), liveWebsite()));

        assertThatThrownBy(() -> websiteService.create(owner,
                new CreateWebsiteRequest("A Third", PageMode.MULTI_PAGE, TemplateType.MENU_ORDERING, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("covers 2 websites");

        verify(websiteRepository, never()).save(any());
    }

    @Test
    void doesNotCountTrashedOrDeletedWebsitesAgainstTheAllowance() {
        // The limit is on what an owner is running, not on everything they have
        // ever made - otherwise deleting one would not free the slot back up.
        businessRules.setDefaultWebsitesPerOwner(2);
        BusinessWebsite trashed = liveWebsite();
        trashed.setStatus(WebsiteStatus.TRASHED);
        BusinessWebsite deleted = liveWebsite();
        deleted.setStatus(WebsiteStatus.DELETED);
        when(websiteRepository.findByOwnerId(owner.accountId()))
                .thenReturn(List.of(liveWebsite(), trashed, deleted));
        when(accountRepository.getReferenceById(owner.accountId())).thenReturn(new com.dbwb.platform.account.entity.Account());
        when(slugGenerator.generateUniqueSlug(any())).thenReturn("a-second");
        when(websiteRepository.save(any())).thenAnswer(inv ->
                TestEntities.withId(inv.getArgument(0), UUID.randomUUID()));

        websiteService.create(owner, new CreateWebsiteRequest("A Second", PageMode.MULTI_PAGE, TemplateType.MENU_ORDERING, null));

        verify(websiteRepository).save(any());
    }

    @Test
    void anAllowanceOfZeroMeansNoLimitAtAll() {
        // The old behaviour, still reachable from config for anyone who wants it.
        businessRules.setDefaultWebsitesPerOwner(0);
        when(websiteRepository.findByOwnerId(owner.accountId()))
                .thenReturn(List.of(liveWebsite(), liveWebsite(), liveWebsite(), liveWebsite()));
        when(accountRepository.getReferenceById(owner.accountId())).thenReturn(new com.dbwb.platform.account.entity.Account());
        when(slugGenerator.generateUniqueSlug(any())).thenReturn("another");
        when(websiteRepository.save(any())).thenAnswer(inv ->
                TestEntities.withId(inv.getArgument(0), UUID.randomUUID()));

        websiteService.create(owner, new CreateWebsiteRequest("Another", PageMode.MULTI_PAGE, TemplateType.MENU_ORDERING, null));

        verify(websiteRepository).save(any());
    }

    private BusinessWebsite liveWebsite() {
        BusinessWebsite existing = TestEntities.withId(new BusinessWebsite(), UUID.randomUUID());
        existing.setStatus(WebsiteStatus.DRAFT);
        return existing;
    }
}
