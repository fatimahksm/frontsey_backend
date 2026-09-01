package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of truth for BRD-derived numeric business rules
 * (Section 10 "Business Rules" + Section 13 "Non-Functional Requirements").
 * Services must read these values from here rather than hardcoding numbers,
 * so a policy change (e.g. grace period length) is a config change only.
 */
@Configuration
@ConfigurationProperties(prefix = "dbwb.business-rules")
public class BusinessRuleProperties {

    /** BR-SUB-006: days a subscription stays active after expiry before the site stops. */
    private int subscriptionGracePeriodDays;

    /** Days a website stays publishable on its free trial, opened at first publish. */
    private int subscriptionTrialDays;

    /**
     * How many websites an owner may have before any of them is on a paid plan.
     *
     * BRD 7.2 says the number is "determined by plan", listed as TBD-003, and
     * the matrix was never settled - so WebsiteService created websites without
     * any limit at all, which is an open invitation to a script. This is the
     * floor: an owner with no active plan gets this many. An owner who holds
     * plans gets the largest maxWebsites among them, which is the plan matrix
     * doing its job once it exists. Zero or less means no limit, which is the
     * old behaviour, deliberately still reachable from config.
     */
    private int defaultWebsitesPerOwner;

    /** BR-AUTH-006: days an account stays disabled (recoverable) before permanent deletion. */
    private int accountDeletionDisableWindowDays;

    /** BR-DATA-004: days a deleted website is restorable before permanent deletion. */
    private int websiteTrashRetentionDays;

    /** BR-MENU-011: days a deleted menu item is restorable before permanent deletion. */
    private int menuItemTrashRetentionDays;

    /**
     * Days an individual analytics event is kept before it is discarded.
     *
     * The table had no retention at all: a row per visit and per item view,
     * written forever. The dashboard's default range is 30 days and its widest
     * export is bounded by whatever range the owner asks for, so keeping a
     * year of raw events covers every question the product can currently ask
     * while stopping the table growing without limit.
     */
    private int analyticsEventRetentionDays;

    /** BR-NFR-001: target public page load time, used only for monitoring/alerting thresholds. */
    private int websitePublicLoadTargetSeconds;

    /** BR-AUTH-002: hours an email-verification link stays valid before it must be re-requested. */
    private int emailVerificationTokenTtlHours;

    /** BR-AUTH-004: minutes a password-reset link stays valid before it must be re-requested. */
    private int passwordResetTokenTtlMinutes;

    /** BR-AUTH-007: days a refresh token stays valid (and keeps extending on use) before its holder must log in again. */
    private int refreshTokenTtlDays;

    /** How often the subscription-lifecycle and suspension-reactivation background jobs run. */
    private long maintenanceJobIntervalMs;

    /** BR-MGR-007: days a manager invitation stays PENDING before it auto-expires. */
    private int managerInvitationExpiryDays;

    public int getAnalyticsEventRetentionDays() {
        return analyticsEventRetentionDays;
    }

    public void setAnalyticsEventRetentionDays(int analyticsEventRetentionDays) {
        this.analyticsEventRetentionDays = analyticsEventRetentionDays;
    }

    public int getDefaultWebsitesPerOwner() {
        return defaultWebsitesPerOwner;
    }

    public void setDefaultWebsitesPerOwner(int defaultWebsitesPerOwner) {
        this.defaultWebsitesPerOwner = defaultWebsitesPerOwner;
    }

    public int getSubscriptionTrialDays() {
        return subscriptionTrialDays;
    }

    public void setSubscriptionTrialDays(int subscriptionTrialDays) {
        this.subscriptionTrialDays = subscriptionTrialDays;
    }

    public int getSubscriptionGracePeriodDays() {
        return subscriptionGracePeriodDays;
    }

    public void setSubscriptionGracePeriodDays(int subscriptionGracePeriodDays) {
        this.subscriptionGracePeriodDays = subscriptionGracePeriodDays;
    }

    public int getAccountDeletionDisableWindowDays() {
        return accountDeletionDisableWindowDays;
    }

    public void setAccountDeletionDisableWindowDays(int accountDeletionDisableWindowDays) {
        this.accountDeletionDisableWindowDays = accountDeletionDisableWindowDays;
    }

    public int getWebsiteTrashRetentionDays() {
        return websiteTrashRetentionDays;
    }

    public void setWebsiteTrashRetentionDays(int websiteTrashRetentionDays) {
        this.websiteTrashRetentionDays = websiteTrashRetentionDays;
    }

    public int getMenuItemTrashRetentionDays() {
        return menuItemTrashRetentionDays;
    }

    public void setMenuItemTrashRetentionDays(int menuItemTrashRetentionDays) {
        this.menuItemTrashRetentionDays = menuItemTrashRetentionDays;
    }

    public int getWebsitePublicLoadTargetSeconds() {
        return websitePublicLoadTargetSeconds;
    }

    public void setWebsitePublicLoadTargetSeconds(int websitePublicLoadTargetSeconds) {
        this.websitePublicLoadTargetSeconds = websitePublicLoadTargetSeconds;
    }

    public int getEmailVerificationTokenTtlHours() {
        return emailVerificationTokenTtlHours;
    }

    public void setEmailVerificationTokenTtlHours(int emailVerificationTokenTtlHours) {
        this.emailVerificationTokenTtlHours = emailVerificationTokenTtlHours;
    }

    public int getPasswordResetTokenTtlMinutes() {
        return passwordResetTokenTtlMinutes;
    }

    public void setPasswordResetTokenTtlMinutes(int passwordResetTokenTtlMinutes) {
        this.passwordResetTokenTtlMinutes = passwordResetTokenTtlMinutes;
    }

    public long getMaintenanceJobIntervalMs() {
        return maintenanceJobIntervalMs;
    }

    public void setMaintenanceJobIntervalMs(long maintenanceJobIntervalMs) {
        this.maintenanceJobIntervalMs = maintenanceJobIntervalMs;
    }

    public int getManagerInvitationExpiryDays() {
        return managerInvitationExpiryDays;
    }

    public void setManagerInvitationExpiryDays(int managerInvitationExpiryDays) {
        this.managerInvitationExpiryDays = managerInvitationExpiryDays;
    }

    public int getRefreshTokenTtlDays() {
        return refreshTokenTtlDays;
    }

    public void setRefreshTokenTtlDays(int refreshTokenTtlDays) {
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }
}
