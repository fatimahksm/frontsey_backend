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

    /** BR-AUTH-006: days an account stays disabled (recoverable) before permanent deletion. */
    private int accountDeletionDisableWindowDays;

    /** BR-DATA-004: days a deleted website is restorable before permanent deletion. */
    private int websiteTrashRetentionDays;

    /** BR-MENU-011: days a deleted menu item is restorable before permanent deletion. */
    private int menuItemTrashRetentionDays;

    /** BR-NFR-001: target public page load time, used only for monitoring/alerting thresholds. */
    private int websitePublicLoadTargetSeconds;

    /** BR-AUTH-002: hours an email-verification link stays valid before it must be re-requested. */
    private int emailVerificationTokenTtlHours;

    /** BR-AUTH-004: minutes a password-reset link stays valid before it must be re-requested. */
    private int passwordResetTokenTtlMinutes;

    /** How often the subscription-lifecycle and suspension-reactivation background jobs run. */
    private long maintenanceJobIntervalMs;

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
}
