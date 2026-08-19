package com.dbwb.platform.admin.dto;

import com.dbwb.platform.subscription.entity.Subscription;
import com.dbwb.platform.subscription.entity.SubscriptionStatus;
import com.dbwb.platform.profile.entity.BusinessProfile;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * BR-ADM-001: the platform-wide view of one website - owner and suspension
 * internals only a Super Admin may see.
 *
 * It used to carry the owner's email and nothing else about them, which made
 * the admin website list an inventory rather than something you could act on:
 * no way to see whose site it was, how many they ran, what they were paying
 * for, or how to reach them without opening a database. Everything an admin
 * needs to decide "should this site be blocked, and who do I call about it" is
 * here now, in one round trip.
 */
public record AdminWebsiteSummaryResponse(
        UUID id,
        String businessName,
        String slug,
        WebsiteStatus status,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        /** From this website's business profile - the number its own customers use. */
        String ownerPhone,
        /** How many websites this owner has in total, so one-site and ten-site owners read differently. */
        int ownerWebsiteCount,
        String planCode,
        String planBillingPeriod,
        SubscriptionStatus subscriptionStatus,
        /** Free access granted by an admin - shown instead of a plan. */
        boolean complimentary,
        Instant subscriptionEndsAt,
        String suspensionReason,
        Instant suspensionReactivateAt,
        Instant publishedAt
) {
    public static AdminWebsiteSummaryResponse from(
            BusinessWebsite w, BusinessProfile profile, Subscription subscription, int ownerWebsiteCount) {
        return new AdminWebsiteSummaryResponse(
                w.getId(), w.getBusinessName(), w.getSlug(), w.getStatus(),
                w.getOwner().getId(), w.getOwner().getEmail(), w.getOwner().getFullName(),
                profile != null ? profile.getPhone() : null,
                ownerWebsiteCount,
                subscription != null && subscription.getPlan() != null ? subscription.getPlan().getCode().name() : null,
                subscription != null && subscription.getPlan() != null ? subscription.getPlan().getBillingPeriod().name() : null,
                subscription != null ? subscription.getStatus() : null,
                subscription != null && subscription.isComplimentary(),
                subscription != null ? subscription.getEndDate() : null,
                w.getSuspensionReason(), w.getSuspensionReactivateAt(), w.getPublishedAt());
    }
}
