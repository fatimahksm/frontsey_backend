package com.dbwb.platform.profile.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** BR-PROF-001..005: owner-managed public contact/profile fields for one website. */
@Entity
@Table(name = "business_profiles")
public class BusinessProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false, unique = true)
    private BusinessWebsite website;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;
    private String coverImageUrl;

    private String phone;
    private String whatsappNumber; // mandatory when orderingMode = WHATSAPP_ORDERING (BR-RULE-013)
    private String email;
    private String address;
    private String googleMapsUrl;
    private String instagramUrl;
    private String tiktokUrl;

    private boolean showPrivacyPolicy;
    @Column(columnDefinition = "TEXT")
    private String privacyPolicyContent;

    private boolean showTermsAndConditions;
    @Column(columnDefinition = "TEXT")
    private String termsAndConditionsContent;

    private boolean showDeliveryPolicy;
    @Column(columnDefinition = "TEXT")
    private String deliveryPolicyContent;

    private boolean showRefundPolicy;
    @Column(columnDefinition = "TEXT")
    private String refundPolicyContent;

    // --- getters / setters ---

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getTiktokUrl() {
        return tiktokUrl;
    }

    public void setTiktokUrl(String tiktokUrl) {
        this.tiktokUrl = tiktokUrl;
    }

    public boolean isShowPrivacyPolicy() {
        return showPrivacyPolicy;
    }

    public void setShowPrivacyPolicy(boolean showPrivacyPolicy) {
        this.showPrivacyPolicy = showPrivacyPolicy;
    }

    public String getPrivacyPolicyContent() {
        return privacyPolicyContent;
    }

    public void setPrivacyPolicyContent(String privacyPolicyContent) {
        this.privacyPolicyContent = privacyPolicyContent;
    }

    public boolean isShowTermsAndConditions() {
        return showTermsAndConditions;
    }

    public void setShowTermsAndConditions(boolean showTermsAndConditions) {
        this.showTermsAndConditions = showTermsAndConditions;
    }

    public String getTermsAndConditionsContent() {
        return termsAndConditionsContent;
    }

    public void setTermsAndConditionsContent(String termsAndConditionsContent) {
        this.termsAndConditionsContent = termsAndConditionsContent;
    }

    public boolean isShowDeliveryPolicy() {
        return showDeliveryPolicy;
    }

    public void setShowDeliveryPolicy(boolean showDeliveryPolicy) {
        this.showDeliveryPolicy = showDeliveryPolicy;
    }

    public String getDeliveryPolicyContent() {
        return deliveryPolicyContent;
    }

    public void setDeliveryPolicyContent(String deliveryPolicyContent) {
        this.deliveryPolicyContent = deliveryPolicyContent;
    }

    public boolean isShowRefundPolicy() {
        return showRefundPolicy;
    }

    public void setShowRefundPolicy(boolean showRefundPolicy) {
        this.showRefundPolicy = showRefundPolicy;
    }

    public String getRefundPolicyContent() {
        return refundPolicyContent;
    }

    public void setRefundPolicyContent(String refundPolicyContent) {
        this.refundPolicyContent = refundPolicyContent;
    }
}
