package com.dbwb.platform.menu;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.menu.dto.AddonGroupRequest;
import com.dbwb.platform.menu.dto.AddonGroupResponse;
import com.dbwb.platform.menu.dto.AddonRequest;
import com.dbwb.platform.menu.dto.BoxVariantRequest;
import com.dbwb.platform.menu.dto.BoxVariantResponse;
import com.dbwb.platform.menu.dto.SizeVariantRequest;
import com.dbwb.platform.menu.dto.SizeVariantResponse;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** BRD 9.7: sizes, add-on groups/add-ons, and fixed-box variants for one menu item. */
@RestController
@RequestMapping("/api/websites/{websiteId}/menu/items/{itemId}")
public class MenuOptionsController {

    private final MenuOptionsService optionsService;
    private final CurrentAccount currentAccount;

    public MenuOptionsController(MenuOptionsService optionsService, CurrentAccount currentAccount) {
        this.optionsService = optionsService;
        this.currentAccount = currentAccount;
    }

    // --- Sizes ---

    @GetMapping("/sizes")
    public ApiResponse<List<SizeVariantResponse>> listSizes(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        return ApiResponse.ok(optionsService.listSizes(websiteId, itemId, currentAccount.get())
                .stream().map(SizeVariantResponse::from).toList());
    }

    @PostMapping("/sizes")
    public ApiResponse<SizeVariantResponse> addSize(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                                     @Valid @RequestBody SizeVariantRequest request) {
        var size = optionsService.addSize(websiteId, itemId, currentAccount.get(), request);
        return ApiResponse.ok(SizeVariantResponse.from(size), "Size added.");
    }

    @DeleteMapping("/sizes/{sizeId}")
    public ApiResponse<Void> deleteSize(@PathVariable UUID websiteId, @PathVariable UUID itemId, @PathVariable UUID sizeId) {
        optionsService.deleteSize(websiteId, itemId, sizeId, currentAccount.get());
        return ApiResponse.ok(null, "Size deleted.");
    }

    // --- Add-on groups & add-ons ---

    @GetMapping("/addon-groups")
    public ApiResponse<List<AddonGroupResponse>> listAddonGroups(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        var groups = optionsService.listAddonGroups(websiteId, itemId, currentAccount.get());
        var response = groups.stream()
                .map(g -> AddonGroupResponse.from(g, optionsService.listAddons(websiteId, itemId, g.getId(), currentAccount.get())))
                .toList();
        return ApiResponse.ok(response);
    }

    @PostMapping("/addon-groups")
    public ApiResponse<AddonGroupResponse> addAddonGroup(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                                          @Valid @RequestBody AddonGroupRequest request) {
        var group = optionsService.addAddonGroup(websiteId, itemId, currentAccount.get(), request);
        return ApiResponse.ok(AddonGroupResponse.from(group, List.of()), "Add-on group added.");
    }

    @DeleteMapping("/addon-groups/{groupId}")
    public ApiResponse<Void> deleteAddonGroup(@PathVariable UUID websiteId, @PathVariable UUID itemId, @PathVariable UUID groupId) {
        optionsService.deleteAddonGroup(websiteId, itemId, groupId, currentAccount.get());
        return ApiResponse.ok(null, "Add-on group deleted.");
    }

    @PostMapping("/addon-groups/{groupId}/addons")
    public ApiResponse<AddonGroupResponse.AddonResponse> addAddon(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                                                    @PathVariable UUID groupId, @Valid @RequestBody AddonRequest request) {
        var addon = optionsService.addAddon(websiteId, itemId, groupId, currentAccount.get(), request);
        return ApiResponse.ok(AddonGroupResponse.AddonResponse.from(addon), "Add-on added.");
    }

    @DeleteMapping("/addon-groups/{groupId}/addons/{addonId}")
    public ApiResponse<Void> deleteAddon(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                          @PathVariable UUID groupId, @PathVariable UUID addonId) {
        optionsService.deleteAddon(websiteId, itemId, groupId, addonId, currentAccount.get());
        return ApiResponse.ok(null, "Add-on deleted.");
    }

    // --- Fixed-box variants ---

    @GetMapping("/box-variants")
    public ApiResponse<List<BoxVariantResponse>> listBoxVariants(@PathVariable UUID websiteId, @PathVariable UUID itemId) {
        return ApiResponse.ok(optionsService.listBoxVariants(websiteId, itemId, currentAccount.get())
                .stream().map(BoxVariantResponse::from).toList());
    }

    @PostMapping("/box-variants")
    public ApiResponse<BoxVariantResponse> addBoxVariant(@PathVariable UUID websiteId, @PathVariable UUID itemId,
                                                          @Valid @RequestBody BoxVariantRequest request) {
        var variant = optionsService.addBoxVariant(websiteId, itemId, currentAccount.get(), request);
        return ApiResponse.ok(BoxVariantResponse.from(variant), "Box variant added.");
    }

    @DeleteMapping("/box-variants/{variantId}")
    public ApiResponse<Void> deleteBoxVariant(@PathVariable UUID websiteId, @PathVariable UUID itemId, @PathVariable UUID variantId) {
        optionsService.deleteBoxVariant(websiteId, itemId, variantId, currentAccount.get());
        return ApiResponse.ok(null, "Box variant deleted.");
    }
}
