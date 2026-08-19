package com.dbwb.platform.plan.dto;

import com.dbwb.platform.plan.entity.PlanCode;
import com.dbwb.platform.plan.entity.TemplatePrice;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.TemplateType;

import java.math.BigDecimal;
import java.util.UUID;

/** One template's price, monthly and yearly. */
public record TemplatePriceResponse(
        UUID id,
        LayoutVariant layoutVariant,
        TemplateType templateType,
        BigDecimal monthlyPrice,
        BigDecimal yearlyPrice,
        PlanCode planCode,
        boolean active
) {
    public static TemplatePriceResponse from(TemplatePrice price) {
        return new TemplatePriceResponse(
                price.getId(), price.getLayoutVariant(), price.getLayoutVariant().templateType(),
                price.getMonthlyPrice(), price.getYearlyPrice(), price.getPlanCode(), price.isActive());
    }
}
