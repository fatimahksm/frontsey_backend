package com.dbwb.platform.admin.dto;

import java.util.UUID;

/** What the admin needs to know after standing a website up for somebody. */
public record ProvisionedWebsiteResponse(
        UUID websiteId,
        String businessName,
        String slug,
        UUID ownerId,
        String ownerEmail,
        /** True when this call created the account, so the admin knows an invitation went out. */
        boolean ownerAccountCreated,
        boolean complimentary
) {
}
