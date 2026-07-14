package com.dbwb.platform.website.dto;

import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.OrderingMode;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.WebsiteStatus;

import java.time.Instant;
import java.util.UUID;

public record WebsiteResponse(
        UUID id,
        String businessName,
        String slug,
        PageMode pageMode,
        OrderingMode orderingMode,
        WebsiteStatus status,
        String primaryLanguage,
        String currency,
        String draftContent,
        String publishedContent,
        Instant publishedAt
) {
    public static WebsiteResponse from(BusinessWebsite w) {
        return new WebsiteResponse(
                w.getId(), w.getBusinessName(), w.getSlug(), w.getPageMode(), w.getOrderingMode(),
                w.getStatus(), w.getPrimaryLanguage(), w.getCurrency(),
                w.getDraftContent(), w.getPublishedContent(), w.getPublishedAt());
    }
}
