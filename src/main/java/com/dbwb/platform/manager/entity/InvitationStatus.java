package com.dbwb.platform.manager.entity;

/** BR-MGR-007: the full manager-invitation lifecycle. Terminal states (REJECTED/REVOKED/EXPIRED) are never re-activated - a fresh invite creates a new row. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    REVOKED,
    EXPIRED
}
