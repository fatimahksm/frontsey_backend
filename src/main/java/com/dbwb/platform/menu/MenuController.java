package com.dbwb.platform.menu;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.menu.dto.CategoryDeletionMode;
import com.dbwb.platform.menu.dto.CategoryDto;
import com.dbwb.platform.menu.dto.MenuItemRequest;
import com.dbwb.platform.menu.dto.MenuItemResponse;
import com.dbwb.platform.menu.entity.ItemAvailability;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/menu")
public class MenuController {

    private final MenuService menuService;
    private final CurrentAccount currentAccount;

    public MenuController(MenuService menuService, CurrentAccount currentAccount) {
        this.menuService = menuService;
        this.currentAccount = currentAccount;
    }

    // --- Categories ---

    @PostMapping("/categories")
    public ApiResponse<CategoryDto> createCategory(@PathVariable UUID websiteId, @RequestParam String name) {
        var category = menuService.createCategory(websiteId, currentAccount.get(), name);
        return ApiResponse.ok(CategoryDto.from(category));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryDto>> listCategories(@PathVariable UUID websiteId) {
        return ApiResponse.ok(menuService.listCategories(websiteId, currentAccount.get())
                .stream().map(CategoryDto::from).toList());
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<CategoryDto> renameCategory(@PathVariable UUID websiteId, @PathVariable UUID categoryId,
                                                    @RequestParam String name) {
        var category = menuService.renameCategory(websiteId, categoryId, currentAccount.get(), name);
        return ApiResponse.ok(CategoryDto.from(category));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID websiteId, @PathVariable UUID categoryId,
                                             @RequestParam CategoryDeletionMode mode,
                                             @RequestParam(required = false) UUID targetCategoryId) {
        menuService.deleteCategory(websiteId, categoryId, currentAccount.get(), mode, targetCategoryId);
        return ApiResponse.ok(null, "Category deleted.");
    }

    // --- Items ---

    @PostMapping("/items")
    public ApiResponse<MenuItemResponse> createItem(@PathVariable UUID websiteId, @Valid @RequestBody MenuItemRequest request) {
        var item = menuService.createItem(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(MenuItemResponse.from(item));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                                     @Valid @RequestBody MenuItemRequest request) {
        var item = menuService.updateItem(websiteId, itemId, currentAccount.get(), request);
        return ApiResponse.ok(MenuItemResponse.from(item));
    }

    @PostMapping("/items/{itemId}/duplicate")
    public ApiResponse<MenuItemResponse> duplicateItem(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        var item = menuService.duplicateItem(websiteId, itemId, currentAccount.get());
        return ApiResponse.ok(MenuItemResponse.from(item));
    }

    @GetMapping("/items")
    public ApiResponse<List<MenuItemResponse>> listItems(@PathVariable UUID websiteId,
                                                          @RequestParam(required = false) UUID categoryId,
                                                          @RequestParam(required = false) String search) {
        var items = menuService.listItems(websiteId, currentAccount.get(), categoryId, search);
        return ApiResponse.ok(items.stream().map(MenuItemResponse::from).toList());
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> trashItem(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        menuService.trashItem(websiteId, itemId, currentAccount.get());
        return ApiResponse.ok(null, "Item moved to trash.");
    }

    @PostMapping("/items/{itemId}/restore")
    public ApiResponse<Void> restoreItem(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        menuService.restoreItem(websiteId, itemId, currentAccount.get());
        return ApiResponse.ok(null, "Item restored.");
    }

    // --- Bulk actions (BR-MENU-010) ---

    @PostMapping("/items/bulk/availability")
    public ApiResponse<Void> bulkAvailability(@PathVariable UUID websiteId,
                                               @RequestParam List<UUID> itemIds,
                                               @RequestParam ItemAvailability availability) {
        menuService.bulkSetAvailability(websiteId, currentAccount.get(), itemIds, availability);
        return ApiResponse.ok(null, "Availability updated.");
    }

    @PostMapping("/items/bulk/trash")
    public ApiResponse<Void> bulkTrash(@PathVariable UUID websiteId, @RequestParam List<UUID> itemIds) {
        menuService.bulkTrash(websiteId, currentAccount.get(), itemIds);
        return ApiResponse.ok(null, "Items moved to trash.");
    }

    @PostMapping("/items/bulk/move-category")
    public ApiResponse<Void> bulkMoveCategory(@PathVariable UUID websiteId,
                                               @RequestParam List<UUID> itemIds,
                                               @RequestParam UUID targetCategoryId) {
        menuService.bulkMoveCategory(websiteId, currentAccount.get(), itemIds, targetCategoryId);
        return ApiResponse.ok(null, "Items moved.");
    }
}
