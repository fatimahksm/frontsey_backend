package com.dbwb.platform.menu;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.MenuItemRequest;
import com.dbwb.platform.menu.entity.Category;
import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private BusinessRuleProperties businessRules;

    private MenuService menuService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        menuService = new MenuService(accessGuard, categoryRepository, menuItemRepository, businessRules);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
    }

    @Test
    void setTemporaryUnavailabilityMarksItemUnavailableUntilTheGivenInstant() {
        MenuItem item = itemWithId();
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        Instant until = Instant.now().plus(2, ChronoUnit.HOURS);

        MenuItem result = menuService.setTemporaryUnavailability(websiteId, item.getId(), caller, until);

        assertThat(result.getAvailability()).isEqualTo(ItemAvailability.UNAVAILABLE);
        assertThat(result.getUnavailableUntil()).isEqualTo(until);
    }

    @Test
    void releaseExpiredTemporaryUnavailabilityRevertsMatchingItemsToAvailable() {
        MenuItem expired = itemWithId();
        expired.setAvailability(ItemAvailability.UNAVAILABLE);
        expired.setUnavailableUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        when(menuItemRepository.findByAvailabilityAndUnavailableUntilBefore(eq(ItemAvailability.UNAVAILABLE), any()))
                .thenReturn(List.of(expired));

        menuService.releaseExpiredTemporaryUnavailability();

        assertThat(expired.getAvailability()).isEqualTo(ItemAvailability.AVAILABLE);
        assertThat(expired.getUnavailableUntil()).isNull();
    }

    @Test
    void bulkSetAvailabilityClearsAnyPendingUnavailableUntil() {
        MenuItem item = itemWithId();
        item.setAvailability(ItemAvailability.UNAVAILABLE);
        item.setUnavailableUntil(Instant.now().plus(1, ChronoUnit.DAYS));
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        menuService.bulkSetAvailability(websiteId, caller, List.of(item.getId()), ItemAvailability.AVAILABLE);

        assertThat(item.getAvailability()).isEqualTo(ItemAvailability.AVAILABLE);
        assertThat(item.getUnavailableUntil()).isNull();
    }

    @Test
    void discountPriceMustBeLessThanRegularPrice() {
        Category category = categoryWithId();
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        MenuItemRequest request = new MenuItemRequest(
                category.getId(), "Latte", null, null, new BigDecimal("4.00"), new BigDecimal("4.00"), null, null, false);

        assertThatThrownBy(() -> menuService.createItem(websiteId, caller, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("less than the regular price");
    }

    @Test
    void discountPriceCannotBeNegative() {
        Category category = categoryWithId();
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        MenuItemRequest request = new MenuItemRequest(
                category.getId(), "Latte", null, null, new BigDecimal("4.00"), new BigDecimal("-1.00"), null, null, false);

        assertThatThrownBy(() -> menuService.createItem(websiteId, caller, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be negative");
    }

    private Category categoryWithId() {
        Category category = new Category();
        category.setWebsite(website);
        category.setName("Drinks");
        return TestEntities.withId(category, UUID.randomUUID());
    }

    private MenuItem itemWithId() {
        MenuItem item = new MenuItem();
        item.setWebsite(website);
        item.setCategory(categoryWithId());
        item.setName("Item");
        item.setPrice(BigDecimal.TEN);
        return TestEntities.withId(item, UUID.randomUUID());
    }
}
