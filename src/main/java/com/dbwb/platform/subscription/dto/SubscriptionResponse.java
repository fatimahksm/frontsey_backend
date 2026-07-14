package com.dbwb.platform.subscription.dto;

import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID websiteId,
        String planCode,
        String billingPeriod,
        SubscriptionStatus status,
        Instant startDate,
        Instant endDate,
        Instant graceEndsAt
) {
    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(), s.getWebsite().getId(), s.getPlan().getCode().name(),
                s.getPlan().getBillingPeriod().name(), s.getStatus(),
                s.getStartDate(), s.getEndDate(), s.getGraceEndsAt());
    }
}
