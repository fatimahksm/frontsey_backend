package com.dbwb.platform.plan.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * 7.2 Subscription Model + BR-ADM-007: Super Admin can edit price, duration,
 * features, and limits of Basic/Premium, but cannot create new plan types
 * in the MVP (enforced by PlanCode being a fixed enum, not free text).
 *
 * TBD-002/TBD-003: actual prices and the full feature/limit matrix are
 * business decisions still pending - these fields are configurable via the
 * admin API rather than hardcoded, so that decision doesn't require a
 * code change when it's finalized.
 */
@Entity
@Table(name = "plans", uniqueConstraints = @UniqueConstraint(columnNames = {"code", "billing_period"}))
public class Plan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanCode code;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // --- Feature / limit matrix (TBD-003) ---
    private int maxWebsites;
    private int maxManagersPerWebsite;
    private int maxLanguages;
    private int maxGalleryImages;
    private long imageStorageLimitMb;
    private boolean analyticsEnabled;
    private boolean multiPageEnabled;

    private boolean active = true;

    // --- getters / setters ---

    public PlanCode getCode() {
        return code;
    }

    public void setCode(PlanCode code) {
        this.code = code;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(BillingPeriod billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getMaxWebsites() {
        return maxWebsites;
    }

    public void setMaxWebsites(int maxWebsites) {
        this.maxWebsites = maxWebsites;
    }

    public int getMaxManagersPerWebsite() {
        return maxManagersPerWebsite;
    }

    public void setMaxManagersPerWebsite(int maxManagersPerWebsite) {
        this.maxManagersPerWebsite = maxManagersPerWebsite;
    }

    public int getMaxLanguages() {
        return maxLanguages;
    }

    public void setMaxLanguages(int maxLanguages) {
        this.maxLanguages = maxLanguages;
    }

    public int getMaxGalleryImages() {
        return maxGalleryImages;
    }

    public void setMaxGalleryImages(int maxGalleryImages) {
        this.maxGalleryImages = maxGalleryImages;
    }

    public long getImageStorageLimitMb() {
        return imageStorageLimitMb;
    }

    public void setImageStorageLimitMb(long imageStorageLimitMb) {
        this.imageStorageLimitMb = imageStorageLimitMb;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }

    public boolean isMultiPageEnabled() {
        return multiPageEnabled;
    }

    public void setMultiPageEnabled(boolean multiPageEnabled) {
        this.multiPageEnabled = multiPageEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
