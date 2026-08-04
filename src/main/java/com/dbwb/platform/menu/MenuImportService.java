package com.dbwb.platform.menu;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.ConfirmImportRequest;
import com.dbwb.platform.menu.dto.DuplicateAction;
import com.dbwb.platform.menu.dto.ImportOutcomeResponse;
import com.dbwb.platform.menu.dto.ImportPreviewResponse;
import com.dbwb.platform.menu.dto.ImportRowDecision;
import com.dbwb.platform.menu.dto.ImportRowResult;
import com.dbwb.platform.menu.dto.ImportRowStatus;
import com.dbwb.platform.menu.entity.Category;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.repository.CategoryRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** BRD 9.8: Excel/CSV menu import with a preview-then-confirm flow (BR-IMP-001..004). */
@Service
public class MenuImportService {

    private final WebsiteAccessGuard accessGuard;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuImportService(WebsiteAccessGuard accessGuard, CategoryRepository categoryRepository, MenuItemRepository menuItemRepository) {
        this.accessGuard = accessGuard;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public ImportPreviewResponse preview(UUID websiteId, AuthenticatedAccount caller, MultipartFile file) {
        accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        List<MenuImportCsvParser.RawRow> rawRows = MenuImportCsvParser.parse(readStream(file));
        List<ImportRowResult> results = rawRows.stream().map(row -> validate(websiteId, row)).toList();
        return ImportPreviewResponse.of(results);
    }

    /**
     * Re-parses and re-validates the same file rather than trusting the
     * client's remembered preview, so a stale preview can never apply
     * different data than what's actually confirmed.
     */
    @Transactional
    public ImportOutcomeResponse confirm(UUID websiteId, AuthenticatedAccount caller, MultipartFile file, ConfirmImportRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU);
        List<MenuImportCsvParser.RawRow> rawRows = MenuImportCsvParser.parse(readStream(file));
        List<ImportRowResult> results = rawRows.stream().map(row -> validate(websiteId, row)).toList();

        boolean hasInvalid = results.stream().anyMatch(r -> r.status() == ImportRowStatus.INVALID);
        if (hasInvalid && !request.importValidRowsOnly()) {
            throw new BusinessRuleViolationException(
                    "The file contains invalid rows. Choose to import only valid rows, or fix the file and re-upload.");
        }

        Map<Integer, DuplicateAction> decisions = new HashMap<>();
        if (request.duplicateDecisions() != null) {
            for (ImportRowDecision d : request.duplicateDecisions()) {
                decisions.put(d.rowNumber(), d.action());
            }
        }

        int created = 0;
        int updated = 0;
        List<ImportRowResult> skipped = new ArrayList<>();

        for (ImportRowResult row : results) {
            switch (row.status()) {
                case INVALID -> skipped.add(row);
                case VALID -> {
                    createItem(website, row);
                    created++;
                }
                case DUPLICATE -> {
                    DuplicateAction action = decisions.getOrDefault(row.rowNumber(), DuplicateAction.SKIP);
                    if (action == DuplicateAction.UPDATE) {
                        updateItem(row);
                        updated++;
                    } else {
                        skipped.add(row);
                    }
                }
            }
        }

        return new ImportOutcomeResponse(created, updated, skipped.size(), skipped);
    }

    private ImportRowResult validate(UUID websiteId, MenuImportCsvParser.RawRow row) {
        List<String> errors = new ArrayList<>();

        String categoryName = row.get("category");
        String name = row.get("name");
        String priceRaw = row.get("price");
        String discountPriceRaw = row.get("discountprice");
        String maxOrderQuantityRaw = row.get("maxorderquantity");

        if (categoryName == null) errors.add("Category is required.");
        if (name == null) errors.add("Name is required.");

        BigDecimal price = null;
        if (priceRaw == null) {
            errors.add("Price is required.");
        } else {
            try {
                price = new BigDecimal(priceRaw);
                if (price.signum() < 0) errors.add("Price cannot be negative.");
            } catch (NumberFormatException e) {
                errors.add("Price '" + priceRaw + "' is not a valid number.");
            }
        }

        BigDecimal discountPrice = null;
        if (discountPriceRaw != null) {
            try {
                discountPrice = new BigDecimal(discountPriceRaw);
                if (price != null && discountPrice.compareTo(price) >= 0) {
                    errors.add("Discount price must be less than the regular price.");
                }
            } catch (NumberFormatException e) {
                errors.add("Discount price '" + discountPriceRaw + "' is not a valid number.");
            }
        }

        Integer maxOrderQuantity = null;
        if (maxOrderQuantityRaw != null) {
            try {
                maxOrderQuantity = Integer.parseInt(maxOrderQuantityRaw);
            } catch (NumberFormatException e) {
                errors.add("Max order quantity '" + maxOrderQuantityRaw + "' is not a valid whole number.");
            }
        }

        ImportRowStatus status;
        UUID existingItemId = null;
        if (!errors.isEmpty()) {
            status = ImportRowStatus.INVALID;
        } else {
            existingItemId = menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, name)
                    .map(MenuItem::getId).orElse(null);
            status = existingItemId != null ? ImportRowStatus.DUPLICATE : ImportRowStatus.VALID;
        }

        return new ImportRowResult(
                row.rowNumber(), categoryName, name, row.get("description"), row.get("ingredients"),
                price, discountPrice, row.get("imageurl"), maxOrderQuantity, status, errors, existingItemId);
    }

    private void createItem(BusinessWebsite website, ImportRowResult row) {
        Category category = categoryRepository.findByWebsiteIdAndNameIgnoreCase(website.getId(), row.categoryName())
                .orElseGet(() -> {
                    Category created = new Category();
                    created.setWebsite(website);
                    created.setName(row.categoryName());
                    return categoryRepository.save(created);
                });

        MenuItem item = new MenuItem();
        item.setWebsite(website);
        item.setCategory(category);
        applyRow(item, row);
        menuItemRepository.save(item);
    }

    private void updateItem(ImportRowResult row) {
        MenuItem item = menuItemRepository.findById(row.existingItemId())
                .orElseThrow(() -> new BusinessRuleViolationException("The item this row was matched to no longer exists."));
        applyRow(item, row);
    }

    private void applyRow(MenuItem item, ImportRowResult row) {
        item.setName(row.name());
        item.setDescription(row.description());
        item.setIngredients(row.ingredients());
        item.setPrice(row.price());
        item.setDiscountPrice(row.discountPrice());
        item.setImageUrl(row.imageUrl());
        item.setMaxOrderQuantity(row.maxOrderQuantity());
    }

    private java.io.InputStream readStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
