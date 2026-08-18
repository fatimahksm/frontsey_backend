package com.dbwb.platform.subscription.dto;

import com.dbwb.platform.plan.entity.BillingPeriod;
import jakarta.validation.constraints.NotNull;

/**
 * Starting checkout for a website.
 *
 * Only the billing period is asked for. The price and the plan both come from
 * the website's own template, so an owner chooses between monthly and yearly
 * and never has to work out which tier they are supposed to be on.
 */
public record CheckoutRequest(@NotNull BillingPeriod billingPeriod) {
}
