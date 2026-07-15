package com.dbwb.platform.analytics.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * BR-AN-001/002: one row per public-site visit or menu-item view. All
 * traffic is counted, including Owner/Manager visits - no exclusion in MVP.
 */
@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID websiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalyticsEventType eventType;

    /** Only set for ITEM_VIEW events. */
    private UUID itemId;

    private String referralSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    public UUID getWebsiteId() {
        return websiteId;
    }

    public void setWebsiteId(UUID websiteId) {
        this.websiteId = websiteId;
    }

    public AnalyticsEventType getEventType() {
        return eventType;
    }

    public void setEventType(AnalyticsEventType eventType) {
        this.eventType = eventType;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getReferralSource() {
        return referralSource;
    }

    public void setReferralSource(String referralSource) {
        this.referralSource = referralSource;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}
