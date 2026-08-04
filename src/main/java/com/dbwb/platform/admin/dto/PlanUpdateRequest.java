package com.dbwb.platform.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** BR-ADM-007: price/features/limits are editable; code+billingPeriod (the plan identity) are not - no new plan types in MVP. */
public record PlanUpdateRequest(
        @NotNull @DecimalMin("0.0") BigDecimal price,
        int maxWebsites,
        int maxManagersPerWebsite,
        int maxLanguages,
        int maxGalleryImages,
        long imageStorageLimitMb,
        boolean analyticsEnabled,
        boolean multiPageEnabled,
        boolean active
) {
}
