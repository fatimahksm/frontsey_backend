package com.dbwb.platform.menu;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.ConfirmImportRequest;
import com.dbwb.platform.menu.dto.DuplicateAction;
import com.dbwb.platform.menu.dto.ImportRowDecision;
import com.dbwb.platform.menu.dto.ImportRowStatus;
import com.dbwb.platform.menu.entity.Category;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuImportServiceTest {

    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private MenuItemRepository menuItemRepository;

    private MenuImportService importService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        importService = new MenuImportService(accessGuard, categoryRepository, menuItemRepository);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
    }

    @Test
    void aWellFormedNewRowIsValid() {
        when(menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, "Espresso"))
                .thenReturn(Optional.empty());

        var preview = importService.preview(websiteId, caller, csv("Category,Name,Price\nCoffee,Espresso,3.00\n"));

        assertThat(preview.validCount()).isEqualTo(1);
        assertThat(preview.rows().get(0).status()).isEqualTo(ImportRowStatus.VALID);
    }

    @Test
    void aRowMatchingAnExistingItemNameIsADuplicate() {
        MenuItem existing = itemWithId("Cappuccino");
        when(menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, "Cappuccino"))
                .thenReturn(Optional.of(existing));

        var preview = importService.preview(websiteId, caller, csv("Category,Name,Price\nCoffee,Cappuccino,4.50\n"));

        assertThat(preview.duplicateCount()).isEqualTo(1);
        assertThat(preview.rows().get(0).existingItemId()).isEqualTo(existing.getId());
    }

    @Test
    void aRowMissingARequiredFieldIsInvalidWithAClearMessage() {
        var preview = importService.preview(websiteId, caller, csv("Category,Name,Price\nCoffee,,3.00\n"));

        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().get(0).errors()).contains("Name is required.");
    }

    @Test
    void confirmRejectsTheWholeImportWhenInvalidRowsExistAndOwnerDidNotOptIntoPartialImport() {
        MockMultipartFile file = csv("Category,Name,Price\nCoffee,,3.00\n");

        assertThatThrownBy(() -> importService.confirm(websiteId, caller, file,
                new ConfirmImportRequest(false, List.of())))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("invalid rows");
    }

    @Test
    void confirmCreatesANewCategoryAndItemForAValidRow() {
        when(menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, "Espresso"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByWebsiteIdAndNameIgnoreCase(websiteId, "Coffee")).thenReturn(Optional.empty());
        Category newCategory = TestEntities.withId(new Category(), UUID.randomUUID());
        when(categoryRepository.save(any())).thenReturn(newCategory);

        var outcome = importService.confirm(websiteId, caller,
                csv("Category,Name,Price\nCoffee,Espresso,3.00\n"), new ConfirmImportRequest(false, List.of()));

        assertThat(outcome.createdCount()).isEqualTo(1);
        verify(menuItemRepository).save(any());
    }

    @Test
    void confirmUpdatesADuplicateOnlyWhenExplicitlyToldTo() {
        MenuItem existing = itemWithId("Cappuccino");
        when(menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, "Cappuccino"))
                .thenReturn(Optional.of(existing));
        when(menuItemRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var outcome = importService.confirm(websiteId, caller,
                csv("Category,Name,Price\nCoffee,Cappuccino,5.00\n"),
                new ConfirmImportRequest(false, List.of(new ImportRowDecision(1, DuplicateAction.UPDATE))));

        assertThat(outcome.updatedCount()).isEqualTo(1);
        assertThat(existing.getPrice()).isEqualByComparingTo("5.00");
    }

    @Test
    void confirmSkipsADuplicateWhenNoDecisionIsGiven() {
        MenuItem existing = itemWithId("Cappuccino");
        when(menuItemRepository.findByWebsiteIdAndNameIgnoreCaseAndTrashedAtIsNull(websiteId, "Cappuccino"))
                .thenReturn(Optional.of(existing));

        var outcome = importService.confirm(websiteId, caller,
                csv("Category,Name,Price\nCoffee,Cappuccino,5.00\n"), new ConfirmImportRequest(false, List.of()));

        assertThat(outcome.skippedCount()).isEqualTo(1);
        assertThat(outcome.createdCount()).isZero();
        assertThat(outcome.updatedCount()).isZero();
        verify(menuItemRepository, never()).save(any());
    }

    private MenuItem itemWithId(String name) {
        MenuItem item = new MenuItem();
        item.setWebsite(website);
        item.setName(name);
        item.setPrice(new BigDecimal("5.00"));
        return TestEntities.withId(item, UUID.randomUUID());
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "menu.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
