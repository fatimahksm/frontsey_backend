package com.dbwb.platform.website.dto;

import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.OrderingMode;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;
import com.dbwb.platform.website.entity.WebsiteStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WebsiteResponse(
        UUID id,
        String businessName,
        String slug,
        PageMode pageMode,
        TemplateType templateType,
        LayoutVariant layoutVariant,
        OrderingMode orderingMode,
        WebsiteStatus status,
        String primaryLanguage,
        String currency,
        String draftContent,
        String publishedContent,
        Instant publishedAt,
        UUID themeId,
        /** This website's own ThemeConfig JSON overriding the preset, or null when it inherits the preset unchanged. */
        String themeConfig,
        /** Phase 4: the caller's role on this website. Null where the caller isn't known to be resolved (action-response endpoints that don't need it). */
        AccessRole role,
        /** For role=MANAGER, exactly the permissions granted; empty for role=OWNER (an owner implicitly has all of them). */
        Set<Permission> permissions
) {
    public static WebsiteResponse from(BusinessWebsite w) {
        return from(w, null, Set.of());
    }

    public static WebsiteResponse from(BusinessWebsite w, AccessRole role, Set<Permission> permissions) {
        return new WebsiteResponse(
                w.getId(), w.getBusinessName(), w.getSlug(), w.getPageMode(), w.getTemplateType(),
                w.getEffectiveLayoutVariant(), w.getOrderingMode(),
                w.getStatus(), w.getPrimaryLanguage(), w.getCurrency(),
                w.getDraftContent(), w.getPublishedContent(), w.getPublishedAt(),
                w.getTheme() != null ? w.getTheme().getId() : null,
                w.getThemeConfig(),
                role, permissions);
    }
}
