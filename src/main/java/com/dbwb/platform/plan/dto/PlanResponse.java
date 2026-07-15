package com.dbwb.platform.plan.dto;

import com.dbwb.platform.plan.entity.BillingPeriod;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.plan.entity.PlanCode;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        PlanCode code,
        BillingPeriod billingPeriod,
        BigDecimal price,
        int maxWebsites,
        int maxManagersPerWebsite,
        int maxLanguages,
        int maxGalleryImages,
        long imageStorageLimitMb,
        boolean analyticsEnabled,
        boolean multiPageEnabled
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getId(), plan.getCode(), plan.getBillingPeriod(), plan.getPrice(),
                plan.getMaxWebsites(), plan.getMaxManagersPerWebsite(), plan.getMaxLanguages(),
                plan.getMaxGalleryImages(), plan.getImageStorageLimitMb(),
                plan.isAnalyticsEnabled(), plan.isMultiPageEnabled());
    }
}
