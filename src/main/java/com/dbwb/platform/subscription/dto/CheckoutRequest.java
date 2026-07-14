package com.dbwb.platform.subscription.dto;

import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.PlanCode;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull PlanCode planCode, @NotNull BillingPeriod billingPeriod) {
}
