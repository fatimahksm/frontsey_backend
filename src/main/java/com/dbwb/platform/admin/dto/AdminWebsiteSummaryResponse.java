package com.dbwb.platform.admin.dto;

import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.WebsiteStatus;

import java.time.Instant;
import java.util.UUID;

/** BR-ADM-001: platform-wide website view - unlike WebsiteResponse, this includes owner/suspension internals only Super Admin may see. */
public record AdminWebsiteSummaryResponse(
        UUID id,
        String businessName,
        String slug,
        WebsiteStatus status,
        UUID ownerId,
        String ownerEmail,
        String suspensionReason,
        Instant suspensionReactivateAt,
        Instant publishedAt
) {
    public static AdminWebsiteSummaryResponse from(BusinessWebsite w) {
        return new AdminWebsiteSummaryResponse(
                w.getId(), w.getBusinessName(), w.getSlug(), w.getStatus(),
                w.getOwner().getId(), w.getOwner().getEmail(),
                w.getSuspensionReason(), w.getSuspensionReactivateAt(), w.getPublishedAt());
    }
}
