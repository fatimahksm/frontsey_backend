package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.portfolio.repository.ServiceItemRepository;
import com.dbwb.platform.profile.entity.BusinessProfile;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.publicapi.PublicWebsiteService;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.subscription.SubscriptionQueryService;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.theme.repository.ThemeRepository;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
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
    @Mock private com.dbwb.platform.audit.AuditService auditService;
    @Mock private PublicWebsiteService publicWebsiteService;

    private WebsiteService websiteService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount owner = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        websiteService = new WebsiteService(
                websiteRepository, themeRepository, accountRepository, profileRepository, categoryRepository,
                serviceItemRepository, slugGenerator, accessGuard, subscriptionQueryService, auditService,
                publicWebsiteService);

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

        assertThat(website.getEffectiveLayoutVariant()).isEqualTo(LayoutVariant.PORTFOLIO_HERO);
    }

    @Test
    void ownerCanSwitchToTheOtherValidLayoutForTheirTemplateType() {
        website.setTemplateType(TemplateType.MENU_ORDERING);

        BusinessWebsite updated = websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.MENU_GRID);

        assertThat(updated.getEffectiveLayoutVariant()).isEqualTo(LayoutVariant.MENU_GRID);
    }

    @Test
    void cannotSwitchToALayoutThatBelongsToTheOtherTemplateType() {
        website.setTemplateType(TemplateType.MENU_ORDERING);

        assertThatThrownBy(() -> websiteService.updateLayoutVariant(websiteId, owner, LayoutVariant.PORTFOLIO_HERO))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not a valid layout");
    }
}
