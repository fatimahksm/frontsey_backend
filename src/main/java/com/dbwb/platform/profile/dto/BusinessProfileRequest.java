package com.dbwb.platform.profile.dto;

/** BR-PROF-001..005: all fields are optional individually - Publish validates the mandatory subset (BR-PUB, TBD-011). */
public record BusinessProfileRequest(
        String description,
        String logoUrl,
        String coverImageUrl,
        String phone,
        String whatsappNumber,
        String email,
        String address,
        String googleMapsUrl,
        String instagramUrl,
        String tiktokUrl,
        boolean showPrivacyPolicy,
        String privacyPolicyContent,
        boolean showTermsAndConditions,
        String termsAndConditionsContent,
        boolean showDeliveryPolicy,
        String deliveryPolicyContent,
        boolean showRefundPolicy,
        String refundPolicyContent
) {
}
