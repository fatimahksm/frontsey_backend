package com.dbwb.platform.profile.dto;

import com.dbwb.platform.profile.entity.BusinessProfile;

public record BusinessProfileResponse(
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
    /** Publish-ready websites always have a profile row, but a brand-new one may not yet - callers get sensible empty defaults. */
    public static BusinessProfileResponse empty() {
        return new BusinessProfileResponse(
                null, null, null, null, null, null, null, null, null, null,
                false, null, false, null, false, null, false, null);
    }

    public static BusinessProfileResponse from(BusinessProfile p) {
        return new BusinessProfileResponse(
                p.getDescription(), p.getLogoUrl(), p.getCoverImageUrl(), p.getPhone(), p.getWhatsappNumber(),
                p.getEmail(), p.getAddress(), p.getGoogleMapsUrl(), p.getInstagramUrl(), p.getTiktokUrl(),
                p.isShowPrivacyPolicy(), p.getPrivacyPolicyContent(),
                p.isShowTermsAndConditions(), p.getTermsAndConditionsContent(),
                p.isShowDeliveryPolicy(), p.getDeliveryPolicyContent(),
                p.isShowRefundPolicy(), p.getRefundPolicyContent());
    }
}
