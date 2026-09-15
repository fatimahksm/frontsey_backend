package com.dbwb.platform.menu;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.CategoryDeletionMode;
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
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @Test
    void createCategoryUnderAParentMakesItASubcategory() {
        Category parent = categoryWithId();
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category created = menuService.createCategory(websiteId, caller, "Iced", parent.getId());

        assertThat(created.getName()).isEqualTo("Iced");
        assertThat(created.getParent()).isEqualTo(parent);
    }

    @Test
    void subcategoriesCannotNestMoreThanOneLevelDeep() {
        Category parent = categoryWithId();
        Category subcategory = categoryWithId();
        subcategory.setParent(parent);
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(subcategory.getId())).thenReturn(Optional.of(subcategory));

        assertThatThrownBy(() -> menuService.createCategory(websiteId, caller, "Decaf", subcategory.getId()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already a sub-category");
    }

    @Test
    void deletingAParentAlsoDeletesItsSubcategoriesAndTrashesEveryItemUnderneath() {
        Category parent = categoryWithId();
        Category subcategory = categoryWithId();
        subcategory.setParent(parent);
        MenuItem itemInSubcategory = itemWithId();
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(categoryRepository.findByParentId(parent.getId())).thenReturn(List.of(subcategory));
        when(menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, parent.getId()))
                .thenReturn(List.of());
        when(menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, subcategory.getId()))
                .thenReturn(List.of(itemInSubcategory));

        menuService.deleteCategory(websiteId, parent.getId(), caller, CategoryDeletionMode.DELETE_ITEMS, null);

        assertThat(itemInSubcategory.getTrashedAt()).isNotNull();
        verify(categoryRepository).delete(subcategory);
        verify(categoryRepository).delete(parent);
    }

    @Test
    void itemsCannotBeMovedIntoASubcategoryThatIsBeingDeletedAlongWithItsParent() {
        Category parent = categoryWithId();
        Category subcategory = categoryWithId();
        subcategory.setParent(parent);
        when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        when(categoryRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(categoryRepository.findByParentId(parent.getId())).thenReturn(List.of(subcategory));
        when(menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, parent.getId()))
                .thenReturn(List.of(itemWithId()));
        when(menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, subcategory.getId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> menuService.deleteCategory(
                websiteId, parent.getId(), caller, CategoryDeletionMode.MOVE_ITEMS_TO_CATEGORY, subcategory.getId()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("being deleted");
    }

    private Category categoryWithId() {
        Category category = new Category();
        category.setWebsite(website);
        category.setName("Drinks");
        return TestEntities.withId(category, UUID.randomUUID());
    }

    // --- one item, and one page of them ---

    @Test
    void getItemReturnsTheItemWhenItBelongsToThisWebsite() {
        MenuItem item = itemWithId();
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThat(menuService.getItem(websiteId, item.getId(), caller)).isSameAs(item);
    }

    /**
     * The id is in the URL and the URL is guessable, so the item's own website
     * decides - not the one the caller put in the path. Without this, an owner
     * who can read their own website could read any item on the platform by
     * pasting its id into their own address.
     */
    @Test
    void getItemRefusesAnItemThatBelongsToAnotherWebsite() {
        MenuItem someoneElses = new MenuItem();
        someoneElses.setWebsite(TestEntities.withId(new BusinessWebsite(), UUID.randomUUID()));
        someoneElses.setName("Not yours");
        someoneElses.setPrice(BigDecimal.ONE);
        MenuItem stored = TestEntities.withId(someoneElses, UUID.randomUUID());
        when(menuItemRepository.findById(stored.getId())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> menuService.getItem(websiteId, stored.getId(), caller))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getItemRefusesAnItemInTheTrash() {
        MenuItem item = itemWithId();
        item.setTrashedAt(java.time.Instant.now());
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> menuService.getItem(websiteId, item.getId(), caller))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listItemsAsksTheDatabaseForOnePageRatherThanEverything() {
        Pageable firstPage = PageRequest.of(0, 50);
        when(menuItemRepository.findByWebsiteIdAndTrashedAtIsNull(websiteId, firstPage))
                .thenReturn(new PageImpl<>(List.of(itemWithId()), firstPage, 500));

        Page<MenuItem> page = menuService.listItems(websiteId, caller, null, null, firstPage);

        // The point of the change: one row back, and the size of the whole list
        // known without having fetched it.
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(500);
        verify(menuItemRepository, never()).findByWebsiteIdAndTrashedAtIsNull(websiteId);
    }

    @Test
    void listItemsPagesWithinACategoryAndWithinASearch() {
        Pageable firstPage = PageRequest.of(0, 10);
        when(menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(eq(websiteId), any(), eq(firstPage)))
                .thenReturn(new PageImpl<>(List.of(), firstPage, 0));
        when(menuItemRepository.findByWebsiteIdAndNameContainingIgnoreCaseAndTrashedAtIsNull(websiteId, "tea", firstPage))
                .thenReturn(new PageImpl<>(List.of(), firstPage, 0));

        menuService.listItems(websiteId, caller, UUID.randomUUID(), null, firstPage);
        menuService.listItems(websiteId, caller, null, "tea", firstPage);

        verify(menuItemRepository).findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(eq(websiteId), any(), eq(firstPage));
        verify(menuItemRepository).findByWebsiteIdAndNameContainingIgnoreCaseAndTrashedAtIsNull(websiteId, "tea", firstPage);
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
