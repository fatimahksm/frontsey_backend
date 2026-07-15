package com.dbwb.platform.menu;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.CategoryDeletionMode;
import com.dbwb.platform.menu.entity.Category;
import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.dto.MenuItemRequest;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Section 9.6/9.7: category and item management. All mutating operations go
 * through WebsiteAccessGuard.requirePermission(MANAGE_MENU) so tenant
 * isolation and Manager permission are enforced uniformly (BR-RULE-003).
 */
@Service
public class MenuService {

    private final WebsiteAccessGuard accessGuard;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final BusinessRuleProperties businessRules;

    public MenuService(
            WebsiteAccessGuard accessGuard,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            BusinessRuleProperties businessRules) {
        this.accessGuard = accessGuard;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.businessRules = businessRules;
    }

    // ----- Categories -----

    @Transactional
    public Category createCategory(UUID websiteId, AuthenticatedAccount caller, String name) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Category category = new Category();
        category.setWebsite(website);
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return categoryRepository.findByWebsiteId(websiteId);
    }

    @Transactional
    public Category renameCategory(UUID websiteId, UUID categoryId, AuthenticatedAccount caller, String newName) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Category category = loadCategory(categoryId, websiteId);
        category.setName(newName);
        return category;
    }

    /** BR-MENU-012: requires an explicit Owner decision when the category still has items. */
    @Transactional
    public void deleteCategory(UUID websiteId, UUID categoryId, AuthenticatedAccount caller,
                                CategoryDeletionMode mode, UUID targetCategoryId) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Category category = loadCategory(categoryId, websiteId);
        long itemCount = menuItemRepository.countByCategoryIdAndTrashedAtIsNull(categoryId);

        if (itemCount > 0) {
            switch (mode) {
                case CANCEL -> throw new BusinessRuleViolationException("Category deletion cancelled.");
                case DELETE_ITEMS -> {
                    List<MenuItem> items = menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, categoryId);
                    items.forEach(i -> i.setTrashedAt(Instant.now()));
                }
                case MOVE_ITEMS_TO_CATEGORY -> {
                    if (targetCategoryId == null) {
                        throw new BusinessRuleViolationException("A target category is required to move items.");
                    }
                    Category target = loadCategory(targetCategoryId, websiteId);
                    List<MenuItem> items = menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, categoryId);
                    items.forEach(i -> i.setCategory(target));
                }
            }
        }
        // BR-MENU-013: no category trash/restore - deletion is immediate.
        categoryRepository.delete(category);
    }

    // ----- Items -----

    @Transactional
    public MenuItem createItem(UUID websiteId, AuthenticatedAccount caller, MenuItemRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Category category = loadCategory(request.categoryId(), websiteId);
        validateDiscount(request.price(), request.discountPrice());

        MenuItem item = new MenuItem();
        item.setWebsite(website);
        item.setCategory(category);
        applyRequest(item, request);
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateItem(UUID websiteId, UUID itemId, AuthenticatedAccount caller, MenuItemRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        validateDiscount(request.price(), request.discountPrice());
        if (!item.getCategory().getId().equals(request.categoryId())) {
            item.setCategory(loadCategory(request.categoryId(), websiteId));
        }
        applyRequest(item, request);
        return item;
    }

    /** BR-MENU-002: duplication copies editable item configuration into a new item. */
    @Transactional
    public MenuItem duplicateItem(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem source = loadItem(itemId, websiteId);

        MenuItem copy = new MenuItem();
        copy.setWebsite(source.getWebsite());
        copy.setCategory(source.getCategory());
        copy.setName(source.getName() + " (Copy)");
        copy.setDescription(source.getDescription());
        copy.setIngredients(source.getIngredients());
        copy.setPrice(source.getPrice());
        copy.setDiscountPrice(source.getDiscountPrice());
        copy.setImageUrl(source.getImageUrl());
        copy.setMaxOrderQuantity(source.getMaxOrderQuantity());
        copy.setFixedBoxItem(source.isFixedBoxItem());
        return menuItemRepository.save(copy);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listItems(UUID websiteId, AuthenticatedAccount caller, UUID categoryFilter, String nameSearch) {
        accessGuard.requireReadAccess(websiteId, caller);
        if (categoryFilter != null) {
            return menuItemRepository.findByWebsiteIdAndCategoryIdAndTrashedAtIsNull(websiteId, categoryFilter);
        }
        if (nameSearch != null && !nameSearch.isBlank()) {
            return menuItemRepository.findByWebsiteIdAndNameContainingIgnoreCaseAndTrashedAtIsNull(websiteId, nameSearch);
        }
        return menuItemRepository.findByWebsiteIdAndTrashedAtIsNull(websiteId);
    }

    /** BR-MENU-011: soft-delete into trash; permanent purge happens via a scheduled job after the retention window. */
    @Transactional
    public void trashItem(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        item.setTrashedAt(Instant.now());
    }

    @Transactional
    public void restoreItem(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        if (item.getTrashedAt() == null) {
            throw new BusinessRuleViolationException("This item is not in trash.");
        }
        int retentionDays = businessRules.getMenuItemTrashRetentionDays();
        if (item.getTrashedAt().plus(retentionDays, ChronoUnit.DAYS).isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("The restore window for this item has expired.");
        }
        item.setTrashedAt(null);
    }

    /** BR-MENU-010: bulk availability change over an explicit list of item ids. */
    @Transactional
    public void bulkSetAvailability(UUID websiteId, AuthenticatedAccount caller, List<UUID> itemIds, ItemAvailability availability) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        itemIds.forEach(id -> {
            MenuItem item = loadItem(id, websiteId);
            item.setAvailability(availability);
            item.setUnavailableUntil(null); // a manual (not time-boxed) change always clears any pending auto-release
        });
    }

    /**
     * BR-MENU-006: marks a single item unavailable until a specific instant
     * (today-only/relative/exact date-time are all just different instants
     * computed by the caller); releaseExpiredTemporaryUnavailability() reverts
     * it automatically once that instant passes.
     */
    @Transactional
    public MenuItem setTemporaryUnavailability(UUID websiteId, UUID itemId, AuthenticatedAccount caller, Instant until) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        item.setAvailability(ItemAvailability.UNAVAILABLE);
        item.setUnavailableUntil(until);
        return item;
    }

    /** BR-MENU-010: bulk deletion (moves to trash, same as single delete). */
    @Transactional
    public void bulkTrash(UUID websiteId, AuthenticatedAccount caller, List<UUID> itemIds) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Instant now = Instant.now();
        itemIds.forEach(id -> loadItem(id, websiteId).setTrashedAt(now));
    }

    /** BR-MENU-010: bulk category movement over an explicit list of item ids. */
    @Transactional
    public void bulkMoveCategory(UUID websiteId, AuthenticatedAccount caller, List<UUID> itemIds, UUID targetCategoryId) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        Category target = loadCategory(targetCategoryId, websiteId);
        itemIds.forEach(id -> loadItem(id, websiteId).setCategory(target));
    }

    /**
     * BR-MENU-006: reverts any item whose temporary-unavailability window has
     * elapsed back to AVAILABLE. Invoked by a scheduled job (see MenuMaintenanceJob).
     */
    @Transactional
    public void releaseExpiredTemporaryUnavailability() {
        menuItemRepository.findByAvailabilityAndUnavailableUntilBefore(ItemAvailability.UNAVAILABLE, Instant.now())
                .forEach(item -> {
                    item.setAvailability(ItemAvailability.AVAILABLE);
                    item.setUnavailableUntil(null);
                });
    }

    // ----- helpers -----

    private void applyRequest(MenuItem item, MenuItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setIngredients(request.ingredients());
        item.setPrice(request.price());
        item.setDiscountPrice(request.discountPrice());
        item.setImageUrl(request.imageUrl());
        item.setMaxOrderQuantity(request.maxOrderQuantity());
    }

    private void validateDiscount(java.math.BigDecimal price, java.math.BigDecimal discountPrice) {
        // BR-RULE-007: discount must be less than regular price and non-negative.
        if (discountPrice != null) {
            if (discountPrice.signum() < 0) {
                throw new BusinessRuleViolationException("Discount price cannot be negative.");
            }
            if (discountPrice.compareTo(price) >= 0) {
                throw new BusinessRuleViolationException("Discount price must be less than the regular price.");
            }
        }
    }

    private Category loadCategory(UUID categoryId, UUID websiteId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
        if (!category.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Category not found.");
        }
        return category;
    }

    private MenuItem loadItem(UUID itemId, UUID websiteId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        if (!item.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Menu item not found.");
        }
        return item;
    }
}
