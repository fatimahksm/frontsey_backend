package com.dbwb.platform.plan.dto;

import com.dbwb.platform.plan.entity.PlanCode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * A Super Admin repricing one template.
 *
 * Both figures are required together rather than patched one at a time: pricing
 * is a pair of numbers an admin decides at once, and letting them drift apart in
 * two requests is how a template ends up costing more yearly than monthly.
 */
public record TemplatePriceUpdateRequest(
        @NotNull @PositiveOrZero @DecimalMax("99999.99") BigDecimal monthlyPrice,
        @NotNull @PositiveOrZero @DecimalMax("999999.99") BigDecimal yearlyPrice,
        @NotNull PlanCode planCode,
        boolean active
) {
}
