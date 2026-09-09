package com.dbwb.platform.publicapi.dto;

import com.dbwb.platform.sections.entity.PageSectionType;
import com.dbwb.platform.theme.dto.ThemeConfig;
import com.dbwb.platform.website.entity.LayoutVariant;
import com.dbwb.platform.website.entity.OrderingMode;
import com.dbwb.platform.website.entity.PageMode;
import com.dbwb.platform.website.entity.TemplateType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated, public-safe view of a published website. Deliberately excludes
 * anything internal (owner id, suspension reason, manager details - Section 14).
 */
public record PublicWebsiteResponse(
        String businessName,
        String slug,
        PageMode pageMode,
        TemplateType templateType,
        LayoutVariant layoutVariant,
        OrderingMode orderingMode,
        String primaryLanguage,
        String currency,
        String publishedContent,
        PublicProfile profile,
        List<PublicOpeningHours> openingHours,
        List<PublicCategory> categories,
        List<PublicDeliveryArea> deliveryAreas,
        /** Populated only for TemplateType.PORTFOLIO sites; empty for MENU_ORDERING. */
        List<PublicService> services,
        List<String> galleryImageUrls,
        PublicSeoMetadata seo,
        /** Owner-added extra sections (About/Testimonials/FAQ/Team), in display order. */
        List<PublicPageSection> sections,
        /**
         * Phase 3: the website's effective design system - its selected Theme's
         * validated config, or {@link ThemeConfig#defaults()} when building from
         * scratch. Single source of truth shared by the public site, the owner's
         * draft preview, and (in future) the published snapshot.
         */
        ThemeConfig theme,
        /**
         * Projects for the PORTFOLIO templates, in the owner's order. Empty for
         * every menu site and for any portfolio that has not added one, so it
         * is always a list and never null.
         */
        List<PublicProject> projects,
        /** Populated only for TemplateType.EVENTS; null on every other kind of website. */
        PublicEvent event,
        /** The running order of the day, in the host's order. Empty for every non-EVENTS site. */
        List<PublicScheduleEntry> schedule
) {
    public record PublicSeoMetadata(String metaTitle, String metaDescription, String ogImageUrl) {
    }

    public record PublicProfile(
            String description, String logoUrl, String coverImageUrl, String phone, String whatsappNumber,
            String email, String address, String googleMapsUrl, String instagramUrl, String tiktokUrl,
            Map<String, String> policies // key: PRIVACY/TERMS/DELIVERY/REFUND -> content, only if shown
    ) {
    }

    public record PublicOpeningHours(String dayOfWeek, boolean open, String opensAt, String closesAt) {
    }

    /**
     * One level of nesting only: a top-level category carries its own
     * un-grouped items plus its sub-categories, and each sub-category's own
     * {@code subcategories} is always empty.
     */
    public record PublicCategory(String id, String name, List<PublicMenuItem> items, List<PublicCategory> subcategories) {
    }

    public record PublicDeliveryArea(String id, String name, java.math.BigDecimal fee,
                                      java.math.BigDecimal minimumOrder, java.math.BigDecimal freeThreshold) {
    }

    public record PublicService(String id, String name, String description, BigDecimal price, String imageUrl) {
    }

    /** `data` is opaque JSON whose shape depends on `type` - parsed on the frontend. */
    public record PublicPageSection(String id, PageSectionType type, String data) {
    }

    /** A project as a visitor sees it - tags already split, nothing internal. */
    /** The facts of the occasion. Every field is optional - the template hides what is absent. */
    public record PublicEvent(
            String eventDate,
            String startTime,
            String endTime,
            String venueName,
            String dressCode,
            String rsvpBy,
            String note) {
    }

    /** One line in the running order - "7:00 PM - Ceremony". */
    public record PublicScheduleEntry(String id, String time, String title, String detail) {
    }

    public record PublicProject(
            String id,
            String name,
            String discipline,
            String year,
            String summary,
            List<String> tags,
            String imageUrl,
            String liveUrl,
            String repoUrl
    ) {
    }
}
