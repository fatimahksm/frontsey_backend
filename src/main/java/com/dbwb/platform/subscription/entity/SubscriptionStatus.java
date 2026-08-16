package com.dbwb.platform.subscription.entity;

/** Section 11.2 Subscription Status model. */
public enum SubscriptionStatus {
    PENDING,
    /**
     * A free window, opened automatically the first time a website is
     * published, so an owner can see their real link working before being
     * asked for money. Publishable like ACTIVE, but it ends flat: there is no
     * grace period after a trial, because nothing was ever paid for.
     */
    TRIAL,
    ACTIVE,
    GRACE,
    EXPIRED,
    CANCELED
}
