package com.dbwb.platform.menu;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.menu.dto.BoxVariantRequest;
import com.dbwb.platform.menu.dto.SizeVariantRequest;
import com.dbwb.platform.menu.entity.MenuItem;
import com.dbwb.platform.menu.repository.AddonGroupRepository;
import com.dbwb.platform.menu.repository.AddonRepository;
import com.dbwb.platform.menu.repository.BoxVariantRepository;
import com.dbwb.platform.menu.repository.MenuItemRepository;
import com.dbwb.platform.menu.repository.SizeVariantRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Covers the item.fixedBoxItem exclusivity rule directly - a real bug this session found the API path had no way to even reach. */
@ExtendWith(MockitoExtension.class)
class MenuOptionsServiceTest {

    @Mock
    private WebsiteAccessGuard accessGuard;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private SizeVariantRepository sizeVariantRepository;
    @Mock
    private AddonGroupRepository addonGroupRepository;
    @Mock
    private AddonRepository addonRepository;
    @Mock
    private BoxVariantRepository boxVariantRepository;

    private MenuOptionsService optionsService;

    private final UUID websiteId = UUID.randomUUID();
    private final AuthenticatedAccount caller = new AuthenticatedAccount(UUID.randomUUID(), "owner@example.com", Role.BUSINESS_OWNER);
    private BusinessWebsite website;

    @BeforeEach
    void setUp() {
        optionsService = new MenuOptionsService(
                accessGuard, menuItemRepository, sizeVariantRepository, addonGroupRepository, addonRepository, boxVariantRepository);
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        lenient().when(accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_MENU)).thenReturn(website);
        lenient().when(sizeVariantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(boxVariantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void sizeCannotBeAddedToAFixedBoxItem() {
        MenuItem item = itemWithId(true);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> optionsService.addSize(websiteId, item.getId(), caller, new SizeVariantRequest("Large", BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("box variants");
    }

    @Test
    void sizeCanBeAddedToASimpleItem() {
        MenuItem item = itemWithId(false);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        var size = optionsService.addSize(websiteId, item.getId(), caller, new SizeVariantRequest("Large", BigDecimal.TEN));

        assertThat(size.getLabel()).isEqualTo("Large");
    }

    @Test
    void boxVariantCannotBeAddedToASimpleItem() {
        MenuItem item = itemWithId(false);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> optionsService.addBoxVariant(websiteId, item.getId(), caller,
                new BoxVariantRequest("Box of 6", 6, new BigDecimal("20.00"))))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("fixed-box item");
    }

    @Test
    void boxVariantCanBeAddedToAFixedBoxItem() {
        MenuItem item = itemWithId(true);
        when(menuItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        var variant = optionsService.addBoxVariant(websiteId, item.getId(), caller,
                new BoxVariantRequest("Box of 6", 6, new BigDecimal("20.00")));

        assertThat(variant.getUnitCount()).isEqualTo(6);
    }

    private MenuItem itemWithId(boolean fixedBoxItem) {
        MenuItem item = new MenuItem();
        item.setWebsite(website);
        item.setName("Item");
        item.setPrice(BigDecimal.TEN);
        item.setFixedBoxItem(fixedBoxItem);
        return TestEntities.withId(item, UUID.randomUUID());
    }
}
