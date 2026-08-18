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
        Instant graceEndsAt,
        /**
         * Whether BR-SUB-010 would currently allow moving to a different plan.
         * Derived here rather than in the frontend so the dashboard can explain
         * a locked plan without keeping its own copy of the rule - and can never
         * offer a switch the server is about to refuse.
         */
        boolean canChangePlan,
        /**
         * Free access granted by a Super Admin. The owner is never billed and
         * nothing expires, so every screen that would otherwise chase them for
         * a payment has to know.
         */
        boolean complimentary
) {
    public static SubscriptionResponse from(Subscription s) {
        boolean lockedIn = s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.GRACE;
        return new SubscriptionResponse(
                s.getId(), s.getWebsite().getId(), s.getPlan().getCode().name(),
                s.getPlan().getBillingPeriod().name(), s.getStatus(),
                s.getStartDate(), s.getEndDate(), s.getGraceEndsAt(), !lockedIn, s.isComplimentary());
    }
}
