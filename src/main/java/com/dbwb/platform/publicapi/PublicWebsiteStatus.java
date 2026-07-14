package com.dbwb.platform.publicapi;

/**
 * BR-QR-003/004: distinguishes "doesn't exist" (branded 404) from
 * "suspended" (generic unavailable) so the frontend renders the right page
 * without ever seeing the internal WebsiteStatus enum or suspension reason.
 */
public enum PublicWebsiteStatus {
    AVAILABLE,
    NOT_FOUND,
    UNAVAILABLE
}
