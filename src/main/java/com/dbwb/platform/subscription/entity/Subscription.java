package com.dbwb.platform.subscription.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.plan.entity.Plan;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 7.2 Subscription Model. One row per website (BR-SUB-002: a website may be
 * created/previewed before subscribing, but not published without one).
 */
@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false, unique = true)
    private BusinessWebsite website;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    private Instant startDate;
    private Instant endDate;

    /** BR-SUB-006: computed as endDate + configured grace period days. */
    private Instant graceEndsAt;

    /**
     * Granted free by a Super Admin: never billed, never expires.
     *
     * Kept as its own flag rather than a SubscriptionStatus value because it is
     * orthogonal to state - it says how this subscription is paid for, not where
     * it stands - and folding it into the enum would mean every status check on
     * the platform had to learn about it.
     */
    @Column(nullable = false)
    private boolean complimentary = false;

    /**
     * Whether this subscription ever served a single day.
     *
     * False for the zero-length trials produced when the configured trial
     * length was missing (endDate landed on startDate, so the next maintenance
     * pass expired it immediately). Such a row is not a subscription the owner
     * has had: it must not count against their one free trial, and it must not
     * freeze the website the way a plan that genuinely ran out does.
     *
     * graceEndsAt is the tell that a payment once succeeded, since
     * SubscriptionService.resolvePaymentOutcome always sets it - so anything
     * ever paid for is "has run", whatever its dates say.
     */
    public boolean hasEverRun() {
        if (graceEndsAt != null) {
            return true;
        }
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

    public boolean isComplimentary() {
        return complimentary;
    }

    public void setComplimentary(boolean complimentary) {
        this.complimentary = complimentary;
    }

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Instant getGraceEndsAt() {
        return graceEndsAt;
    }

    public void setGraceEndsAt(Instant graceEndsAt) {
        this.graceEndsAt = graceEndsAt;
    }
}
