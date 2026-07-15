package com.dbwb.platform.menu;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.AddonGroupRequest;
import com.dbwb.platform.menu.dto.AddonRequest;
import com.dbwb.platform.menu.dto.BoxVariantRequest;
import com.dbwb.platform.menu.dto.SizeVariantRequest;
import com.dbwb.platform.menu.entity.Addon;
import com.dbwb.platform.menu.entity.AddonGroup;
import com.dbwb.platform.menu.entity.BoxVariant;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.entity.SizeVariant;
import com.dbwb.platform.menu.repository.AddonGroupRepository;
import com.dbwb.platform.menu.repository.AddonRepository;
import com.dbwb.platform.menu.repository.BoxVariantRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.repository.SizeVariantRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * BRD 9.7: per-item Sizes, Add-on Groups/Add-ons, and Fixed-Box variants.
 * An item is either a simple item (optional SizeVariants) or a fixed-box
 * item (BoxVariants) - never both (see MenuItem.fixedBoxItem javadoc) -
 * enforced here since MenuService only manages the item's own fields.
 */
@Service
public class MenuOptionsService {

    private final WebsiteAccessGuard accessGuard;
    private final MenuItemRepository menuItemRepository;
    private final SizeVariantRepository sizeVariantRepository;
    private final AddonGroupRepository addonGroupRepository;
    private final AddonRepository addonRepository;
    private final BoxVariantRepository boxVariantRepository;

    public MenuOptionsService(
            WebsiteAccessGuard accessGuard,
            MenuItemRepository menuItemRepository,
            SizeVariantRepository sizeVariantRepository,
            AddonGroupRepository addonGroupRepository,
            AddonRepository addonRepository,
            BoxVariantRepository boxVariantRepository) {
        this.accessGuard = accessGuard;
        this.menuItemRepository = menuItemRepository;
        this.sizeVariantRepository = sizeVariantRepository;
        this.addonGroupRepository = addonGroupRepository;
        this.addonRepository = addonRepository;
        this.boxVariantRepository = boxVariantRepository;
    }

    // --- Sizes (BR-OPT-001) ---

    @Transactional(readOnly = true)
    public List<SizeVariant> listSizes(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return sizeVariantRepository.findByMenuItemId(loadItem(itemId, websiteId).getId());
    }

    @Transactional
    public SizeVariant addSize(UUID websiteId, UUID itemId, AuthenticatedAccount caller, SizeVariantRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        if (item.isFixedBoxItem()) {
            throw new BusinessRuleViolationException("A fixed-box item uses box variants, not sizes.");
        }
        SizeVariant size = new SizeVariant();
        size.setMenuItem(item);
        size.setLabel(request.label());
        size.setPrice(request.price());
        return sizeVariantRepository.save(size);
    }

    @Transactional
    public void deleteSize(UUID websiteId, UUID itemId, UUID sizeId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        loadItem(itemId, websiteId);
        SizeVariant size = sizeVariantRepository.findById(sizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Size not found."));
        if (!size.getMenuItem().getId().equals(itemId)) {
            throw new ResourceNotFoundException("Size not found.");
        }
        sizeVariantRepository.delete(size);
    }

    // --- Add-on groups & add-ons (BR-OPT-002/003) ---

    @Transactional(readOnly = true)
    public List<AddonGroup> listAddonGroups(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return addonGroupRepository.findByMenuItemId(loadItem(itemId, websiteId).getId());
    }

    @Transactional(readOnly = true)
    public List<Addon> listAddons(UUID websiteId, UUID itemId, UUID groupId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        loadItem(itemId, websiteId);
        return addonRepository.findByAddonGroupId(groupId);
    }

    @Transactional
    public AddonGroup addAddonGroup(UUID websiteId, UUID itemId, AuthenticatedAccount caller, AddonGroupRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        AddonGroup group = new AddonGroup();
        group.setMenuItem(item);
        group.setName(request.name());
        group.setMaxSelections(request.maxSelections());
        return addonGroupRepository.save(group);
    }

    @Transactional
    public void deleteAddonGroup(UUID websiteId, UUID itemId, UUID groupId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        loadItem(itemId, websiteId);
        AddonGroup group = loadAddonGroup(groupId, itemId);
        addonRepository.findByAddonGroupId(groupId).forEach(addonRepository::delete);
        addonGroupRepository.delete(group);
    }

    @Transactional
    public Addon addAddon(UUID websiteId, UUID itemId, UUID groupId, AuthenticatedAccount caller, AddonRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        loadItem(itemId, websiteId);
        AddonGroup group = loadAddonGroup(groupId, itemId);
        Addon addon = new Addon();
        addon.setAddonGroup(group);
        addon.setName(request.name());
        addon.setExtraPrice(request.extraPrice());
        return addonRepository.save(addon);
    }

    @Transactional
    public void deleteAddon(UUID websiteId, UUID itemId, UUID groupId, UUID addonId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        loadItem(itemId, websiteId);
        loadAddonGroup(groupId, itemId);
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not found."));
        if (!addon.getAddonGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Add-on not found.");
        }
        addonRepository.delete(addon);
    }

    // --- Fixed-box variants (BR-OPT-004/005) ---

    @Transactional(readOnly = true)
    public List<BoxVariant> listBoxVariants(UUID websiteId, UUID itemId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return boxVariantRepository.findByMenuItemId(loadItem(itemId, websiteId).getId());
    }

    @Transactional
    public BoxVariant addBoxVariant(UUID websiteId, UUID itemId, AuthenticatedAccount caller, BoxVariantRequest request) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        MenuItem item = loadItem(itemId, websiteId);
        if (!item.isFixedBoxItem()) {
            throw new BusinessRuleViolationException("Box variants can only be added to a fixed-box item.");
        }
        BoxVariant variant = new BoxVariant();
        variant.setMenuItem(item);
        variant.setLabel(request.label());
        variant.setUnitCount(request.unitCount());
        variant.setPrice(request.price());
        return boxVariantRepository.save(variant);
    }

    @Transactional
    public void deleteBoxVariant(UUID websiteId, UUID itemId, UUID variantId, AuthenticatedAccount caller) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        loadItem(itemId, websiteId);
        BoxVariant variant = boxVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Box variant not found."));
        if (!variant.getMenuItem().getId().equals(itemId)) {
            throw new ResourceNotFoundException("Box variant not found.");
        }
        boxVariantRepository.delete(variant);
    }

    private MenuItem loadItem(UUID itemId, UUID websiteId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found."));
        if (!item.getWebsite().getId().equals(websiteId)) {
            throw new ResourceNotFoundException("Menu item not found.");
        }
        return item;
    }

    private AddonGroup loadAddonGroup(UUID groupId, UUID itemId) {
        AddonGroup group = addonGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on group not found."));
        if (!group.getMenuItem().getId().equals(itemId)) {
            throw new ResourceNotFoundException("Add-on group not found.");
        }
        return group;
    }
}
