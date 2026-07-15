package com.dbwb.platform.publicapi.dto;

import com.dbwb.platform.website.entity.OrderingMode;
import com.dbwb.platform.website.entity.PageMode;

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
        OrderingMode orderingMode,
        String primaryLanguage,
        String currency,
        String publishedContent,
        PublicProfile profile,
        List<PublicOpeningHours> openingHours,
        List<PublicCategory> categories,
        List<PublicDeliveryArea> deliveryAreas,
        List<String> galleryImageUrls,
        PublicSeoMetadata seo
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

    public record PublicCategory(String id, String name, List<PublicMenuItem> items) {
    }

    public record PublicDeliveryArea(String id, String name, java.math.BigDecimal fee,
                                      java.math.BigDecimal minimumOrder, java.math.BigDecimal freeThreshold) {
    }
}
