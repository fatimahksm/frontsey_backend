package com.dbwb.platform.plan;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.plan.repository.TemplatePriceRepository;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.TemplateType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * template_prices.active was already the Super Admin's switch, but it only ever
 * reached pricing. Turning a template off left it visible in the picker,
 * selectable, and creatable - the owner met the wall only at checkout, after
 * they had already chosen and built on it.
 */
@ExtendWith(MockitoExtension.class)
class TemplateAvailabilityTest {

    @Mock
    private TemplatePriceRepository repository;

    private TemplateAvailability availability() {
        return new TemplateAvailability(repository);
    }

    private TemplatePrice price(LayoutVariant variant, boolean active) {
        TemplatePrice price = new TemplatePrice();
        price.setLayoutVariant(variant);
        price.setMonthlyPrice(new BigDecimal("9.99"));
        price.setYearlyPrice(new BigDecimal("99.99"));
        price.setPlanCode(PlanCode.BASIC);
        price.setActive(active);
        return price;
    }

    @Test
    void refusesATemplateTheAdminHasSwitchedOff() {
        when(repository.findByLayoutVariant(LayoutVariant.MENU_GRID))
                .thenReturn(Optional.of(price(LayoutVariant.MENU_GRID, false)));

        assertThat(availability().isOffered(LayoutVariant.MENU_GRID)).isFalse();
        assertThatThrownBy(() -> availability().requireOffered(LayoutVariant.MENU_GRID))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not available to choose");
    }

    @Test
    void allowsOneTheAdminHasLeftOn() {
        when(repository.findByLayoutVariant(LayoutVariant.MENU_GRID))
                .thenReturn(Optional.of(price(LayoutVariant.MENU_GRID, true)));

        assertThatCode(() -> availability().requireOffered(LayoutVariant.MENU_GRID)).doesNotThrowAnyException();
    }

    @Test
    void treatsATemplateWithNoPriceRowAsNotOffered() {
        // The safer reading: an unpriced template is one nobody can be charged
        // for, so letting a website onto it only produces an unpayable site.
        when(repository.findByLayoutVariant(LayoutVariant.MENU_BISTRO)).thenReturn(Optional.empty());

        assertThat(availability().isOffered(LayoutVariant.MENU_BISTRO)).isFalse();
    }

    @Test
    void refusesAKindOfWebsiteWithNothingLeftOnOffer() {
        // Every portfolio template switched off - so no portfolio website can be
        // created, rather than one being created onto a withdrawn default.
        when(repository.findAllByActiveTrueOrderByLayoutVariantAsc())
                .thenReturn(List.of(price(LayoutVariant.MENU_GRID, true)));

        TemplateAvailability availability = availability();
        assertThat(availability.hasAnyOffered(TemplateType.MENU_ORDERING)).isTrue();
        assertThat(availability.hasAnyOffered(TemplateType.PORTFOLIO)).isFalse();
        assertThatThrownBy(() -> availability.requireAnyOffered(TemplateType.PORTFOLIO))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("No templates of that kind");
    }

    @Test
    void offersOnlyTheActiveOnes() {
        when(repository.findAllByActiveTrueOrderByLayoutVariantAsc())
                .thenReturn(List.of(price(LayoutVariant.MENU_CLASSIC, true), price(LayoutVariant.MENU_GRID, true)));

        assertThat(availability().offeredVariants())
                .containsExactlyInAnyOrder(LayoutVariant.MENU_CLASSIC, LayoutVariant.MENU_GRID);
    }
}
